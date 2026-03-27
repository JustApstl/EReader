# Android E-Reader Technical Summary

This document is a handoff summary of the current Android e-reader app so work can continue in a fresh chat/session without rebuilding context from scratch.

## 1. Architecture Overview

The app is split into four main layers:

- `data/format`: format detection and metadata extraction.
- `data/repository`: library, reader, browse, export, analytics, accessibility, cloud, and MOBI conversion logic.
- `data/local`: Room entities/DAOs and DataStore-backed preferences.
- `ui/`: Compose-based Home, Browse, Reader, Settings, and overlay surfaces.

Primary entry points:

- `app/src/main/java/com/dyu/ereader/ui/home/screens/HomeScreen.kt`
- `app/src/main/java/com/dyu/ereader/ui/reader/screens/ReaderScreen.kt`
- `app/src/main/java/com/dyu/ereader/ui/browse/screens/BrowseCatalogScreen.kt`

## 2. Multi-Format Reading Architecture

Core format model:

- `app/src/main/java/com/dyu/ereader/data/model/library/BookItem.kt`

Currently modeled `BookType` values:

- `EPUB`
- `EPUB3`
- `PDF`
- `AZW3`
- `MOBI`
- `CBZ`
- `CBR`

Format registry:

- `app/src/main/java/com/dyu/ereader/data/format/BookFormatRegistry.kt`

Registered handlers:

- `EpubFormatHandler`
- `PdfFormatHandler`
- `Azw3FormatHandler`
- `MobiFormatHandler`
- `CbzFormatHandler`
- `CbrFormatHandler`

### EPUB / EPUB3

EPUB is the most complete in-app reader path.

- Reader UI: `app/src/main/java/com/dyu/ereader/ui/reader/formats/epub/EpubJsReader.kt`
- JS bridge: `app/src/main/java/com/dyu/ereader/ui/reader/formats/epub/ReaderBridge.kt`
- JS assets: `app/src/main/assets/reader/`

Implementation notes:

- Uses a WebView hosting `epub.js`.
- Bridge exposes book bytes as base64 to JS.
- Supports progress + CFI tracking, pagination, TOC loading, search, text extraction, annotations, image taps, external links, TTS hooks, and reader setting synchronization.
- Reader settings are serialized into JSON and applied inside the WebView runtime.

### PDF

PDF has a dedicated native reader path:

- `app/src/main/java/com/dyu/ereader/ui/reader/formats/pdf/PdfReaderScreen.kt`

Implementation notes:

- Uses Android `PdfRenderer`.
- Copies the source PDF into app cache first.
- Supports both scroll mode and paged mode.
- Tracks current page, total pages, and normalized progress.
- Uses a bitmap cache for rendered pages.

### MOBI

MOBI is handled through conversion:

- Conversion repo: `app/src/main/java/com/dyu/ereader/data/repository/mobi/MobiConversionRepository.kt`
- Temporary reader screen/fallback UI: `app/src/main/java/com/dyu/ereader/ui/reader/formats/mobi/MobiReaderScreen.kt`
- Conversion trigger: `app/src/main/java/com/dyu/ereader/ui/reader/viewmodel/ReaderViewModel.kt`

Implementation notes:

- Attempts native conversion with `MobiNative.extractToEpubDir(...)`.
- Repackages extracted content as an EPUB in cache.
- Falls back to a configured remote conversion server if native conversion fails.
- On success, reader state swaps to `resolvedBookType = EPUB` and loads the converted file through the EPUB path.

### AZW3

AZW3 is recognized in the library and browse layers but is not yet fully supported in-app.

- Placeholder: `app/src/main/java/com/dyu/ereader/ui/reader/formats/azw3/Azw3ReaderScreen.kt`

Current behavior:

- Shows an unsupported/conversion guidance screen.

### CBZ / CBR

Comics/manga archive formats are recognized by the library/import/browse layers:

- `app/src/main/java/com/dyu/ereader/data/format/handlers/CbzFormatHandler.kt`
- `app/src/main/java/com/dyu/ereader/data/format/handlers/CbrFormatHandler.kt`

Current status:

- They are importable/detectable as book types.
- They do not yet have a dedicated in-app comic reader surface.
- They currently fall through to unsupported at reader-routing level.

### TXT / HTML

TXT and HTML are currently only partially surfaced:

- Detection/scoring exists in browse helpers:
  - `app/src/main/java/com/dyu/ereader/data/repository/browse/BrowseRepositoryDownloadHelpers.kt`

Current status:

- `txt` and `html` can be inferred when downloading from browse sources.
- They are not yet modeled in `BookType`.
- They are not yet registered in `BookFormatRegistry`.
- They do not yet have an in-app reader route.

## 3. Reader Routing

Format dispatch lives in:

- `app/src/main/java/com/dyu/ereader/ui/reader/screens/ReaderScreenFormatContent.kt`

Current routing:

- `PDF` -> `PdfReaderScreen`
- `bookType.isEpub` -> `EpubJsReader`
- `AZW3` -> placeholder screen
- `MOBI` -> fallback screen, with actual conversion initiated in `ReaderViewModel`
- everything else -> unsupported screen

## 4. Library Management System

Primary repository:

- `app/src/main/java/com/dyu/ereader/data/repository/library/LibraryRepository.kt`

Scanner:

- `app/src/main/java/com/dyu/ereader/data/local/scanner/LibraryScanner.kt`

Persistence:

- Room books table: `app/src/main/java/com/dyu/ereader/data/local/db/BookEntity.kt`
- Preferences/DataStore: `app/src/main/java/com/dyu/ereader/data/local/prefs/ReaderPreferencesStore.kt`

### Folder Access and Scanning

The library uses Android SAF:

- User selects a folder via `OpenDocumentTree`.
- Persisted read/write permission is stored with `takePersistableUriPermission(...)`.
- `LibraryScanner.scanTree(...)` walks the selected SAF tree with `DocumentFile`.
- Only files whose extensions appear in `BookFormatRegistry.supportedExtensions` are included.

### Book Identity and Persistence

Each scanned/imported file gets a stable ID:

- `stableMd5(uri.toString())`

Stored metadata includes:

- URI
- file name
- title
- author
- cover image
- favorite flag
- last opened
- date added
- file type
- description/publisher/date/isbn/language/year
- file size
- genres
- countries

### Re-scan Behavior

During a scan:

- existing entries are matched by stable ID,
- file URI/name/size are refreshed,
- EPUB metadata can be re-read if detailed fields are missing,
- favorites and reading state are preserved through handler merge logic,
- missing books are removed from Room unless they are still valid local file entries.

### Import / Copy Into Library

Import paths:

- `importBook(file: File)` for direct local import.
- `importBookToLibrary(file, libraryTreeUri)` to copy a file into the selected library SAF folder and then insert it into Room.

This is also how downloaded browse content is integrated into the local library.

### Delete

Delete behavior supports:

- `content://` via `DocumentFile.delete()`
- `file://` via `File.delete()`

## 5. Library Collections, Favorites, and Recents

Collections model:

- `app/src/main/java/com/dyu/ereader/data/model/library/BookCollection.kt`

Collections storage:

- DataStore-backed JSON list in `ReaderPreferencesStore`

Available collection operations:

- create collection
- toggle a book into/out of a collection
- remove deleted books from all collections
- delete collection

Favorites:

- Stored in Room on the book entity.
- Toggled through `LibraryRepository.toggleFavorite(...)`.

Recent reading:

- Derived from `lastOpened > 0`
- Sorted with in-progress items first, then most recently opened, then progress/date

## 6. Filtering and Sorting Logic

Home state contract:

- `app/src/main/java/com/dyu/ereader/ui/home/state/HomeContract.kt`

Filtering implementation:

- `app/src/main/java/com/dyu/ereader/ui/home/state/HomeBookFilters.kt`

### Search Scopes

- `ALL`
- `TITLE`
- `AUTHOR`
- `LANGUAGE`
- `YEAR`
- `EXTENSION`

### Structured Filters

The app currently filters by:

- book type / format
- genres
- language
- year
- country
- reading status (`UNREAD`, `IN_PROGRESS`, `FINISHED`)

### Sorting

Sort orders implemented:

- title
- author
- date added
- last opened
- progress
- file size

### Derived Data in Home ViewModel

`HomeViewModelObservers` combines:

- library books flow
- stored collections
- display preferences
- per-book progress

and derives:

- `allBooks`
- `visibleBooks`
- `recentBooks`
- `availableGenres`
- `availableLanguages`
- `availableYears`
- `availableCountries`
- collection shelves

## 7. Current Home / Library UI

Primary files:

- `app/src/main/java/com/dyu/ereader/ui/home/screens/HomeScreen.kt`
- `app/src/main/java/com/dyu/ereader/ui/home/screens/HomeScreenHeader.kt`
- `app/src/main/java/com/dyu/ereader/ui/home/components/HomeLibraryContent.kt`
- `app/src/main/java/com/dyu/ereader/ui/home/overlays/sheets/FilterBottomSheet.kt`

Current library UI includes:

- top header with search + filter
- inline search bar
- feed tabs: `All`, `Recent`, `Favorites`, `Collections`
- volume toolbar with title, grid/list toggle, and sort selector
- recent reading featured card
- genre exploration section
- favorites section
- collection shelves
- grid/list rendering of books
- add-book surfaces
- pull-to-refresh scanning
- filter chips for active filters

## 8. Reader UI and Features Already Built

Primary reader shell:

- `app/src/main/java/com/dyu/ereader/ui/reader/screens/ReaderScreen.kt`
- `app/src/main/java/com/dyu/ereader/ui/reader/chrome/ReaderChrome.kt`

Reader data/state:

- `app/src/main/java/com/dyu/ereader/ui/reader/state/ReaderState.kt`
- `app/src/main/java/com/dyu/ereader/data/repository/reader/ReaderRepository.kt`

Already-built reader features:

- top and bottom reader chrome
- docked panels for:
  - settings
  - search
  - listen/TTS
  - accessibility
  - analytics
  - export
- bookmark support
- highlight support
- margin note support
- image zoom dialog
- TOC panel / chapter navigation
- progress tracking
- page count / pagination callbacks
- onboarding state
- per-book reader settings
- tap-zone actions
- 3D page turn settings and page transition style settings
- reader theme/font/background/image/filter customization
- external link handling and image click handling through the EPUB bridge

Reader annotation entities:

- `app/src/main/java/com/dyu/ereader/data/local/db/AnnotationEntities.kt`

## 9. File Handling Methods Already Built

### SAF and Content Resolver

The app already uses:

- `ActivityResultContracts.OpenDocumentTree`
- `ActivityResultContracts.OpenDocument`
- `ActivityResultContracts.CreateDocument`
- `contentResolver.openInputStream(...)`
- `contentResolver.openOutputStream(...)`
- `DocumentFile.fromTreeUri(...)`
- `DocumentFile.fromSingleUri(...)`

### WebView Bridge File Handling

The EPUB bridge already supports:

- loading the entire book as base64
- loading custom background image as a data URL
- loading custom font as a data URL
- opening external links
- receiving search results / chapters / pagination from JS

### Download + Import Pipeline

Browse download flow:

- OPDS catalogs are defined in `BrowseRepositoryProfiles.kt`
- download options are inferred/scored in `BrowseRepositoryDownloadHelpers.kt`
- downloads are queued in `HomeViewModelBrowseActions.kt`
- completed files are imported into the library through `LibraryRepository.importBookToLibrary(...)`

Formats inferred by browse download helpers:

- `epub`
- `epub3`
- `pdf`
- `cbz`
- `cbr`
- `mobi`
- `azw3`
- `txt`
- `html`

## 10. Settings / Backup / Export

Settings screen:

- `app/src/main/java/com/dyu/ereader/ui/home/settings/SettingsScreen.kt`

Already built:

- app theme
- app font
- accent color
- liquid glass toggle
- navigation bar style
- reader-control toggles
- reader-control order drag/reorder
- library display settings
- backup export/import through JSON document pickers

Annotation/data export:

- `app/src/main/java/com/dyu/ereader/data/repository/export/ExportRepository.kt`

Supported export formats:

- PDF
- Markdown
- JSON

Current export content:

- highlights
- bookmarks
- notes

## 11. Known Current Gaps / Caveats

These are important if continuing work in a fresh chat:

- `TextAlignment.ORIGINAL` reset behavior still needs fixing.
- `TXT` support is only partial and not fully wired into the library + reader pipeline.
- `AZW3` is recognized but not readable in-app yet.
- `CBZ` / `CBR` are recognized/importable but do not yet have a dedicated comic/manga reader UI.
- EPUB remains the most feature-complete reading path.
- A lot of advanced reader customization is implemented around the EPUB WebView + JS bridge, not around non-EPUB formats.

## 12. Best Files To Read First In A Fresh Session

- `app/src/main/java/com/dyu/ereader/data/model/library/BookItem.kt`
- `app/src/main/java/com/dyu/ereader/data/format/BookFormatRegistry.kt`
- `app/src/main/java/com/dyu/ereader/data/repository/library/LibraryRepository.kt`
- `app/src/main/java/com/dyu/ereader/ui/home/state/HomeBookFilters.kt`
- `app/src/main/java/com/dyu/ereader/ui/home/components/HomeLibraryContent.kt`
- `app/src/main/java/com/dyu/ereader/ui/reader/screens/ReaderScreen.kt`
- `app/src/main/java/com/dyu/ereader/ui/reader/screens/ReaderScreenFormatContent.kt`
- `app/src/main/java/com/dyu/ereader/ui/reader/formats/epub/EpubJsReader.kt`
- `app/src/main/java/com/dyu/ereader/ui/reader/formats/epub/ReaderBridge.kt`
- `app/src/main/java/com/dyu/ereader/ui/reader/viewmodel/ReaderViewModel.kt`
- `app/src/main/java/com/dyu/ereader/data/local/prefs/ReaderPreferencesStore.kt`

