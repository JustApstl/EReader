# File Map

This is the current high-signal project map after the recent package and UI cleanup.
It is meant to be readable and maintainable, not a dump of every file in the repo.

## App Entry

- `app/src/main/java/com/dyu/ereader/ui/app/EReaderApplication.kt`
  - Hilt application entry point.
- `app/src/main/java/com/dyu/ereader/ui/app/MainActivity.kt`
  - Activity host for the Compose app.
- `app/src/main/java/com/dyu/ereader/ui/app/AppNavHost.kt`
  - Top-level navigation shell.
- `app/src/main/java/com/dyu/ereader/ui/app/MainViewModel.kt`
  - App-level side effects such as auth callback routing.

## Core

- `app/src/main/java/com/dyu/ereader/core/codec/`
  - Navigation argument helpers.
- `app/src/main/java/com/dyu/ereader/core/crypto/`
  - Hashing and stable ID helpers.
- `app/src/main/java/com/dyu/ereader/core/locale/`
  - Locale-aware formatting helpers.
- `app/src/main/java/com/dyu/ereader/core/logging/`
  - App logging utilities.
- `app/src/main/java/com/dyu/ereader/core/net/`
  - URL normalization and network helpers.

## Data Layer

### Format

- `app/src/main/java/com/dyu/ereader/data/format/BookFormatRegistry.kt`
  - Supported format registry and extension matching.
- `app/src/main/java/com/dyu/ereader/data/format/handlers/`
  - Format handlers for EPUB, PDF, AZW3, MOBI, CBZ, and CBR.
- `app/src/main/java/com/dyu/ereader/data/format/epub/EpubMetadataReader.kt`
  - EPUB metadata/page count helpers.
- `app/src/main/java/com/dyu/ereader/data/format/mobi/MobiNative.kt`
  - JNI bridge for native MOBI extraction.

### Local Storage

- `app/src/main/java/com/dyu/ereader/data/local/db/`
  - Room entities, DAOs, database, and converters.
- `app/src/main/java/com/dyu/ereader/data/local/prefs/ReaderPreferencesStore.kt`
  - DataStore-backed app, library, reader, backup, and notification preferences.
- `app/src/main/java/com/dyu/ereader/data/local/scanner/LibraryScanner.kt`
  - SAF folder scanning and library import discovery.

### Models

- `app/src/main/java/com/dyu/ereader/data/model/app/`
  - App theme and accent models.
- `app/src/main/java/com/dyu/ereader/data/model/library/`
  - Library book and collection models.
- `app/src/main/java/com/dyu/ereader/data/model/reader/`
  - Reader settings, controls, and reading presets.
- `app/src/main/java/com/dyu/ereader/data/model/browse/`
  - OPDS browse/feed models.
- `app/src/main/java/com/dyu/ereader/data/model/cloud/`
  - Cloud account and backup scope models.

### Repositories

- `app/src/main/java/com/dyu/ereader/data/repository/library/LibraryRepository.kt`
  - Library import, scan, merge, delete, and metadata refresh.
- `app/src/main/java/com/dyu/ereader/data/repository/reader/ReaderRepository.kt`
  - Reader settings, progress, annotations, and reading persistence.
- `app/src/main/java/com/dyu/ereader/data/repository/browse/`
  - Browse parsing, catalog profiles, validation, and download inference.
- `app/src/main/java/com/dyu/ereader/data/repository/export/ExportRepository.kt`
  - Export of annotations/bookmarks/notes.
- `app/src/main/java/com/dyu/ereader/data/repository/mobi/MobiConversionRepository.kt`
  - MOBI conversion flow.
- `app/src/main/java/com/dyu/ereader/data/repository/notifications/`
  - Reading reminder scheduling and receivers.
- `app/src/main/java/com/dyu/ereader/data/repository/cloud/`
  - Cloud backup abstractions and provider-specific integrations.

## Dependency Injection

- `app/src/main/java/com/dyu/ereader/di/modules/data/`
  - Database and storage providers.
- `app/src/main/java/com/dyu/ereader/di/modules/repositories/`
  - Repository bindings.

## UI Layer

### Shared UI

- `app/src/main/java/com/dyu/ereader/ui/components/add/`
  - Add/import UI surfaces.
- `app/src/main/java/com/dyu/ereader/ui/components/badges/`
  - Shared badges such as `BetaBadge`.
- `app/src/main/java/com/dyu/ereader/ui/components/books/`
  - Shared book metadata/about surfaces.
- `app/src/main/java/com/dyu/ereader/ui/components/buttons/`
  - Shared chrome/action buttons.
- `app/src/main/java/com/dyu/ereader/ui/components/cards/BookCard.kt`
  - Shared grid and list book cards.
- `app/src/main/java/com/dyu/ereader/ui/components/dialogs/`
  - Shared dialog defaults and color picker.
- `app/src/main/java/com/dyu/ereader/ui/components/inputs/`
  - Shared search bar, segmented defaults, slider defaults.
- `app/src/main/java/com/dyu/ereader/ui/components/menus/`
  - Shared dropdown menu surfaces.
- `app/src/main/java/com/dyu/ereader/ui/components/surfaces/`
  - Shared section surfaces and liquid-glass styling.

### App Theme

- `app/src/main/java/com/dyu/ereader/ui/app/theme/Theme.kt`
  - Material 3 color schemes and theme host.
- `app/src/main/java/com/dyu/ereader/ui/app/theme/AppThemeResolvers.kt`
  - Accent/theme resolution.
- `app/src/main/java/com/dyu/ereader/ui/app/theme/UiTokens.kt`
  - Shared shape/spacing tokens.

### Home

- `app/src/main/java/com/dyu/ereader/ui/home/screens/HomeScreen.kt`
  - Main Library/Browse/Settings/Logs route.
- `app/src/main/java/com/dyu/ereader/ui/home/screens/root/HomeScreenContent.kt`
  - Root shell content wiring.
- `app/src/main/java/com/dyu/ereader/ui/home/navigation/HomeNavigation.kt`
  - Home navigation contracts.
- `app/src/main/java/com/dyu/ereader/ui/home/components/`
  - Home feed cards, empty states, and shared home content.
- `app/src/main/java/com/dyu/ereader/ui/home/components/library/`
  - Library-specific feed/search/genre components.
- `app/src/main/java/com/dyu/ereader/ui/home/overlays/sheets/`
  - Filter sheet and book action sheet.
- `app/src/main/java/com/dyu/ereader/ui/home/settings/`
  - App settings surfaces and settings sections.
- `app/src/main/java/com/dyu/ereader/ui/home/settings/SettingsReaderDefaultsComponents.kt`
  - Extracted helper components for reader-default settings UI.
- `app/src/main/java/com/dyu/ereader/ui/home/settings/cloud/`
  - Cloud backup screen, cards, and support helpers.
- `app/src/main/java/com/dyu/ereader/ui/home/settings/cloud/CloudSyncSupport.kt`
  - Shared provider labels, auth messages, connectivity state, and cloud helper utilities.
- `app/src/main/java/com/dyu/ereader/ui/home/viewmodel/`
  - Home view model plus split action/observer helpers.

### Browse

- `app/src/main/java/com/dyu/ereader/ui/browse/screens/BrowseCatalogScreen.kt`
  - Browse route entry.
- `app/src/main/java/com/dyu/ereader/ui/browse/components/`
  - Catalog, feed, queue, and dialog components.
- `app/src/main/java/com/dyu/ereader/ui/browse/sheets/BrowseBookDetailSheet.kt`
  - Browse book details modal.

### Reader

- `app/src/main/java/com/dyu/ereader/ui/reader/screens/ReaderScreen.kt`
  - Reader route host.
- `app/src/main/java/com/dyu/ereader/ui/reader/screens/ReaderScreenFormatContent.kt`
  - Format routing to EPUB/PDF/MOBI/AZW3/unsupported surfaces.
- `app/src/main/java/com/dyu/ereader/ui/reader/formats/`
  - Format-specific reader implementations.
- `app/src/main/java/com/dyu/ereader/ui/reader/chrome/`
  - Reader top/bottom chrome and panel appearance helpers.
- `app/src/main/java/com/dyu/ereader/ui/reader/controls/`
  - Reader action bars, dropdowns, and dialogs.
- `app/src/main/java/com/dyu/ereader/ui/reader/overlays/`
  - Reader sheets, menus, overlays, and docked panels.
- `app/src/main/java/com/dyu/ereader/ui/reader/settings/`
  - Reader settings sheet and settings-specific controls/tabs.
- `app/src/main/java/com/dyu/ereader/ui/reader/viewmodel/`
  - Reader view model plus split helpers for annotations, listen, and exports.

## Assets

- `app/src/main/assets/reader/`
  - EPUB WebView reader shell.
- `app/src/main/assets/reader/css/`
  - Reader CSS split into `core`, `features`, and `render`.
- `app/src/main/assets/reader/js/`
  - Reader runtime split into `core`, `features`, `render`, `styles`, and `vendor`.
- `app/src/main/assets/licenses/libmobi.LICENSE`
  - Native MOBI dependency license.

## Native

- `app/src/main/cpp/CMakeLists.txt`
  - Native build configuration.
- `app/src/main/cpp/mobi_native.cpp`
  - MOBI native bridge.
- `app/src/main/cpp/third_party/libmobi`
  - Vendored native dependency.

## Docs

- `docs/FILE_MAP.md`
  - This structure guide.
- `docs/TECHNICAL_SUMMARY.md`
  - Fresh-session handoff summary.

## Key Starting Points

If you are continuing work in a new session, start here:

- `app/src/main/java/com/dyu/ereader/data/model/library/BookItem.kt`
- `app/src/main/java/com/dyu/ereader/data/format/BookFormatRegistry.kt`
- `app/src/main/java/com/dyu/ereader/data/repository/library/LibraryRepository.kt`
- `app/src/main/java/com/dyu/ereader/ui/home/screens/HomeScreen.kt`
- `app/src/main/java/com/dyu/ereader/ui/home/settings/SettingsScreen.kt`
- `app/src/main/java/com/dyu/ereader/ui/reader/screens/ReaderScreen.kt`
- `app/src/main/java/com/dyu/ereader/ui/reader/formats/epub/EpubJsReader.kt`
- `app/src/main/java/com/dyu/ereader/ui/reader/viewmodel/ReaderViewModel.kt`
