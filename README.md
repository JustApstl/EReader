# EReader

Android e-reader built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt, `epub.js`, native PDF rendering, and MOBI conversion support.

## Current State

- Multi-format library with EPUB, EPUB3, PDF, AZW3, MOBI, CBZ, and CBR recognition
- EPUB is the most complete in-app reading path
- PDF uses native `PdfRenderer`
- MOBI converts to EPUB before reading
- AZW3 is recognized but still unsupported for in-app reading
- CBZ and CBR are recognized/importable but do not yet have a comic reader
- Local backup/export is the primary dependable backup flow today
- Cloud backup UI is present, but only WebDAV is currently practical without extra provider registration

## Project Structure

- `app/src/main/java/com/dyu/ereader/core`
  - Core helpers for codec, crypto, locale, logging, and networking
- `app/src/main/java/com/dyu/ereader/data`
  - Format handlers, local persistence, models, repositories
- `app/src/main/java/com/dyu/ereader/di`
  - Hilt modules
- `app/src/main/java/com/dyu/ereader/ui/app`
  - App entry, nav host, theming
- `app/src/main/java/com/dyu/ereader/ui/home`
  - Library, browse, settings, logs, and home view model
- `app/src/main/java/com/dyu/ereader/ui/browse`
  - OPDS catalog/feed UI
- `app/src/main/java/com/dyu/ereader/ui/reader`
  - Reader screens, chrome, overlays, settings, formats, and reader view model
- `app/src/main/java/com/dyu/ereader/ui/components`
  - Shared Compose components

For a more useful structure guide, see [docs/FILE_MAP.md](/home/dyu/Project/docs/FILE_MAP.md).

## Important Entry Points

- `app/src/main/java/com/dyu/ereader/ui/app/MainActivity.kt`
- `app/src/main/java/com/dyu/ereader/ui/app/AppNavHost.kt`
- `app/src/main/java/com/dyu/ereader/ui/home/screens/HomeScreen.kt`
- `app/src/main/java/com/dyu/ereader/ui/home/settings/SettingsScreen.kt`
- `app/src/main/java/com/dyu/ereader/ui/reader/screens/ReaderScreen.kt`
- `app/src/main/java/com/dyu/ereader/ui/reader/screens/ReaderScreenFormatContent.kt`
- `app/src/main/java/com/dyu/ereader/data/repository/library/LibraryRepository.kt`
- `app/src/main/java/com/dyu/ereader/data/repository/reader/ReaderRepository.kt`

## Reading Features

- EPUB reader via WebView + `epub.js`
- Native PDF screen with page and scroll modes
- Reader progress tracking and last-opened state
- Highlights, bookmarks, margin notes, and export
- Search inside EPUB content
- Reader appearance customization:
  - background
  - font
  - font size
  - line spacing
  - margins
  - alignment
  - page transition style
- Listen / TTS controls
- Reader settings preview inside App Settings

## Library Features

- SAF-based library folder access
- Format registry-driven scanning/import
- Grid/list layouts
- Filtering by:
  - type
  - genre
  - language
  - year
  - country
  - reading status
- Favorites and collections
- OPDS browse + download import pipeline

## Settings

- App theme defaults to `System`
- Material 3 theme cleanup is in progress across the whole app
- App appearance and reader defaults are separated
- Notifications menu exists for reminder-related controls
- Backup is split between local backup/export flows and in-progress cloud backup UI

## Backup

### Local Backup

- Export/import is the main reliable backup path right now
- Intended backup content includes:
  - app settings
  - reader settings
  - bookmarks
  - notes
  - highlights

### Cloud Backup

- WebDAV is the only provider path that does not depend on provider app registration
- Dropbox and Google Drive are visible in the UI but intentionally disabled until provider registration/OAuth setup is ready
- Cloud backup code is still under cleanup and should be treated as in-progress

## Theming Direction

The current cleanup direction is:

- Material 3 first
- fewer custom boxed surfaces
- less OLED-specific branching in shared UI
- stronger alignment between Library, Browse, Reader, and Settings
- system-based theme/accent behavior where possible

## Build

```bash
./gradlew :app:compileDebugKotlin
./gradlew assembleDebug
```

## Recommended Validation

- Open an EPUB and a PDF and verify reader settings still apply
- Check Library grid/list, filtering, and recent reading surfaces
- Verify Settings search and navigation flows
- Export and import a local backup
- Test browse download -> import -> read flow

## Docs

- [docs/FILE_MAP.md](/home/dyu/Project/docs/FILE_MAP.md)
- [docs/TECHNICAL_SUMMARY.md](/home/dyu/Project/docs/TECHNICAL_SUMMARY.md)
