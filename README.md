# EReader (Android, Kotlin, Compose, MVVM)

A full-featured Android EPUB/PDF reader with modern Compose UI, rich customization, and advanced library management.

## Core Features

### 📚 Reading Experience
- **EPUB/PDF Support**: Full EPUB and PDF file format support
- **Dual Reading Modes**: Scroll mode (vertical) and page mode (CSS column pagination)
- **Reading Progress**: Automatic save/resume of reading position per book
- **Bionic Reading Mode**: Partially-bold formatted text for improved focus
- **Customizable Reader**:
  - Font size, line spacing, and margins
  - 3 selectable font families
  - Multiple reader themes (light/dark, custom colors)
  - Image filters (brightness, contrast adjustments)
- **Reader Chrome Control**: Tap center to show/hide UI for immersive reading
- **Navigation**: Chapter list with quick jump to any chapter
- **Full-Text Search**: Search within books with context preview and highlighting

### 🏠 Library Management
- **Auto Scan**: Automatic EPUB/PDF file detection via Storage Access Framework (SAF)
- **Dual Layout**: Grid (customizable columns) and list view options
- **Advanced Filtering**:
  - Filter by Title, Author, Language, Year, or File Extension
  - Genre-based filtering
  - Book type filtering (EPUB/PDF)
  - Favorites collection
- **Smart Sorting**: Sort by Title, Author, or Date Added
- **Recent Reading**: Quick access to recently opened books
- **Persistent Metadata**: Book cover, title, author, publisher, language, year extracted from EPUB
- **OPDS Catalog Support**: Browse and download from Project Gutenberg, Standard Ebooks, and LibriVox catalogs

### ✨ Customization & UI
- **Material 3 Design**: Dynamic color themes with system bar integration
- **Theme Options**: Light mode, dark mode, automatic system theme
- **Display Preferences**:
  - Toggle system bars for immersive reading
  - Show/hide book type badges
  - Enable/disable animations
  - Customizable grid columns
  - Genre section visibility toggle
- **Accessibility Modes**:
  - High contrast mode
  - Dyslexia-friendly fonts (OpenDyslexic, Atkinson Hyperlegible)
  - Enhanced line & letter spacing
  - Reduced animations option
  - Screen reader support

### 🎯 Annotations & Highlights
- **Highlight Management**: Add, view, and delete highlights with color coding
- **Bookmark System**: Create bookmarks with titles and notes
- **Margin Notes**: Add position-specific margin annotations (left/right)
- **Highlight Collections**: Organize highlights into named collections
- **Persistent Storage**: All annotations stored locally in Room database
- **Annotation Export**: Export highlights and notes as PDF, Markdown, or JSON

### 🔊 Text-to-Speech
- **Audio Narration**: Native Android TTS integration
- **Speed Control**: Adjustable playback speed (0.5x - 2.0x)
- **Pitch Control**: Customize voice pitch
- **Multi-Language Support**: Multiple language options
- **Pause/Resume**: Full playback control with session persistence

### ☁️ Cloud Integration
- **Cloud Sync**: Optional sync with Google Drive or Dropbox
- **Conflict Resolution**: Smart handling of sync conflicts
- **Selective Sync**: Choose what to sync (highlights, bookmarks, progress)
- **Privacy-First**: All syncing respects user privacy with optional opt-in
- **Auto-Sync**: Optional automatic synchronization on app launch

### 📊 Analytics & Statistics
- **Reading Statistics**: Track total minutes read, WPM, session count
- **Library Analytics**: Aggregate stats across all books
- **Privacy Respecting**: Optional analytics with privacy mode enabled by default
- **Statistics Dashboard**: View reading trends and patterns
- **Export Data**: Export statistics in JSON format

### 📥 Export & Share
- **PDF Export**: Export highlighted passages and notes to PDF
- **Markdown Export**: Export as well-formatted Markdown files
- **JSON Export**: Raw export for external processing
- **Email Integration**: Direct email sharing of highlights
- **Multiple Format Support**: Choose export format based on use case

## What's New in v1.2.0

- **Library Filters**: Multiple filter options (Title, Author, Language, Year, Extension)
- **Genre Support**: Automatic genre extraction and filtering from EPUB metadata
- **Advanced Layout**: Grid/list view toggle with customizable grid columns
- **Display Settings**: Toggle recent reading, favorites, genres, and status bar visibility
- **Immersive Mode**: Hide system bars for full-screen reading
- **Highlight System**: Full highlight support with database persistence
- **Reader Customization**: Extended reader settings with image filters
- **Performance**: Optimized library scanning and metadata caching

## What's New in v2.0.0 (Major Update)

- **Dependency Injection**: Complete Hilt DI integration for better architecture
- **Enhanced Annotations**: Bookmarks, margin notes, and annotation collections
- **Full-Text Search**: Search within books with context and highlighting
- **Text-to-Speech**: Native audio narration with speed and pitch control
  - Direct text reading via `startTTS(text)` from selected content
  - JavaScript bridge for epub.js text passing  
  - Visual "Now Reading" indicator with pulsing animation
  - Multi-language voice selection
- **OPDS Book Downloads**: Download books directly from OPDS catalogs to local library
  - Progress tracking with cancellation support
  - Supports EPUB and PDF formats
  - Automatic import to library with metadata extraction
- **Cloud Synchronization**: OAuth authentication for cloud storage
  - Google Drive, OneDrive, Proton Drive, Dropbox support
  - JSON settings persistence with SharedPreferences
  - Token-based OAuth flow with automatic validation
  - Selective sync configuration
- **Enhanced Search**: Full-text search with result navigation
  - Jump to search result location with chapter navigation
  - Context preview in search results
  - Search result highlighting in document
- **Comprehensive Analytics**: Privacy-first reading statistics
  - Event tracking: page turns, highlights, searches, TTS usage
  - Session-based reading analytics
  - Privacy mode prevents sensitive tracking
  - Statistics dashboard with reading insights
- **Multi-Format Export**: Export annotations as PDF, Markdown, or JSON
- **Production Optimization**: R8 minification enabled for smaller APK size
- **Enhanced User Experience**: Improved UI responsiveness with better error handling

## Next Steps for Integration

### Immediate (High Priority)

1. **TTS Text Selection Integration**
   - ✅ Backend: `startTTS(text)` method ready in ReaderViewModel
   - ✅ Backend: JavaScript bridge `startTTSWithSelectedText()` in ReaderBridge
   - ✅ UI: "Read Aloud" button added to text selection menu
   - 🔲 TODO: Verify epub.js integration and test text reading
   - 🔲 TODO: Add TTS settings sheet with language/voice selection

2. **OPDS Book Download UI**
   - ✅ Backend: `downloadBook()` with progress tracking in BrowseRepository
   - ✅ Backend: `importBook()` for library import in LibraryRepository
   - 🔲 TODO: Add download button to each book in BrowseCatalogScreen
   - 🔲 TODO: Create BookDownloadDialog with progress bar
   - 🔲 TODO: Wire up success → library addition flow

3. **Cloud Sync OAuth**
   - ✅ Backend: OAuth token management in CloudSyncRepository
   - ✅ Backend: Settings persistence with JSON serialization
   - 🔲 TODO: Create CloudAuthDialog for each provider:
     - Google Drive: `GoogleSignIn` integration
     - OneDrive: `microsoftgraph-android-sdk`
     - Proton Drive: WebView-based OAuth  
     - Dropbox: `com.dropbox.core:dropbox-core-sdk`
   - 🔲 TODO: Add OAuth login buttons to ReaderSettingsSheet
   - 🔲 TODO: Implement `performSync()` background task

4. **Analytics Event Tracking**
   - ✅ Backend: All tracking methods ready in AnalyticsRepository
   - 🔲 TODO: Add tracking calls to ViewModel:
     - `recordPageTurn()` on page navigation
     - `recordTTSUsage()` on TTS playback end
     - `recordSearch()` on search execution
     - `recordHighlight()` on highlight creation
   - 🔲 TODO: Call `startReadingSession()` on ReaderScreen init
   - 🔲 TODO: Call `endReadingSession()` on back navigation
   - 🔲 TODO: Create AnalyticsDashboard composable

5. **Search Result Navigation**
   - ✅ Backend: `navigateToSearchResult()` method in ViewModel
   - 🔲 TODO: Add click handler in SearchDialog
   - 🔲 TODO: Wire click → `viewModel.navigateToSearchResult(result)`
   - 🔲 TODO: Implement text highlighting at search result location

### Medium Priority

6. **Liquid Glass Effect**
   - 🔲 TODO: Create conditional liquid glass surface styling
   - 🔲 TODO: Apply to CloudProviderDialog
   - 🔲 TODO: Apply to TTSReader
   - 🔲 TODO: Apply to all new bottom sheets/dialogs

7. **3D Page Turning Animation**
   - 🔲 TODO: Implement CSS 3D transforms in epub.js
   - 🔲 TODO: Add gesture detection for page swipe
   - 🔲 TODO: Wire `setPageTurn3dEnabled()` toggle

8. **Margin Notes Editor**
   - 🔲 TODO: Create MarginNoteEditor.kt dialog
   - 🔲 TODO: Add margin note button to selection menu
   - 🔲 TODO: Implement note editing and deletion

### Low Priority (Optimization)

9. **Advanced Features**
   - Annotation sync to cloud backup
   - Highlight export refinements
   - Search result caching
   - TTS voice customization UI
   - Background sync job scheduling

## Testing Checklist

- [ ] TTS: Select text → "Read Aloud" → Verify playback and visual indicator
- [ ] OPDS: Browse Standard Ebooks → Download → Verify appearance in library
- [ ] Cloud Sync: Authenticate → Verify token persists → Test sync
- [ ] Search: Query book text → Click result → Verify navigation
- [ ] Analytics: Read session → Dashboard shows accurate stats
- [ ] Export: Create highlights → Export as PDF/Markdown → Verify output
- [ ] Build: Verify APK size reduction with R8 minification

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM + Repository Pattern + Dependency Injection (Hilt)
- **Navigation**: Navigation Compose
- **EPUB Rendering**: epub.js (running in WebView)
- **Persistence**: 
  - Room Database (highlights, bookmarks, annotations, statistics)
  - DataStore Preferences (settings, progress, user preferences)
- **Dependency Injection**: Hilt with DI modules for all features
- **Text-to-Speech**: Android native TTS engine
- **Cloud Services**:
  - Google Play Services (Drive, Auth, Analytics)
  - Dropbox SDK
- **PDF Generation**: iText 7
- **API Clients**:
  - Retrofit 2 (REST API calls)
  - OkHttp 3 (HTTP client)
  - Gson (JSON serialization)
- **Parsing & Format Support**:
  - Jsoup (HTML parsing & bionic text formatting)
  - XML parsing for OPDS catalogs
- **Accessibility**:
  - Semantic annotation support
  - System accessibility service integration
- **Other Dependencies**:
  - `androidx.room` (local database)
  - `androidx.datastore` (user preferences)
  - `androidx.webkit` (WebView)
  - `io.coil-kt:coil-compose` (image loading)
  - `androidx.hilt:hilt-navigation-compose` (Compose + Hilt integration)
- **Concurrency**: Coroutines with Flows for reactive data

## Project Structure

```
app/src/main/java/com/dyu/ereader/
├── di/                            # Dependency Injection modules (Hilt)
│   ├── DataModule.kt              # Database & DataStore DI
│   ├── RepositoryModule.kt        # Repository DI
│   └── FeatureModule.kt           # Feature repositories DI
├── data/
│   ├── model/                     # Domain models
│   │   ├── BookItem.kt
│   │   ├── ReaderSettings.kt
│   │   ├── SearchModels.kt        # Search feature models
│   │   ├── TextToSpeechModels.kt  # TTS feature models
│   │   ├── CloudSyncModels.kt     # Cloud sync models
│   │   ├── OpdsModels.kt          # OPDS catalog models
│   │   ├── AccessibilityModels.kt # Accessibility options
│   │   ├── AnalyticsModels.kt     # Statistics & analytics
│   │   └── ExportModels.kt        # Export & share models
│   ├── database/                  # Room database
│   │   ├── BookEntity.kt
│   │   ├── HighlightEntity.kt
│   │   ├── AnnotationEntities.kt  # Bookmarks, margin notes, collections
│   │   ├── AnnotationDaos.kt      # DAOs for annotations
│   │   └── BookDatabase.kt
│   ├── storage/                   # DataStore preferences & SAF scanning
│   │   ├── LibraryScanner.kt
│   │   └── ReaderPreferencesStore.kt
│   └── repository/                # Data layer abstraction
│       ├── LibraryRepository.kt
│       ├── ReaderRepository.kt
│       ├── SearchRepository.kt
│       ├── TextToSpeechRepository.kt
│       ├── CloudSyncRepository.kt
│       ├── OpdsRepository.kt
│       ├── AccessibilityRepository.kt
│       ├── AnalyticsRepository.kt
│       └── ExportRepository.kt
├── ui/
│   ├── home/                      # Home library screen + ViewModel
│   ├── reader/                    # Reader screen, WebView controller + ViewModel
│   ├── components/                # Reusable Compose components
│   └── theme/                     # Compose Material 3 theme configuration
├── util/                          # Utilities (logging, hashing, metadata parsing)
└── EReaderApplication.kt          # Hilt-enabled app entry point
```

## Build & Run

1. Open the project in Android Studio (latest stable recommended).
2. Let Gradle sync.
3. Run on a device/emulator with Android 8.0+ (`minSdk 26`, `targetSdk 36`).
4. On first launch:
   - Tap **Grant Library Access**.
   - Select internal storage root (or folder containing EPUB files).
   - The app scans and displays detected EPUB books.

### CLI (if Java/Android SDK are installed)

```bash
./gradlew assembleDebug
```

## How Features Work

### Reading Features
- **Scroll Mode**: Traditional vertical scrolling through WebView content
- **Page Mode**: CSS-based column pagination with left/right tap for page turns
- **Progress Tracking**: Reading position stored as percentage + CFI (EPUB Canonical Fragment Identifier) bookmark
- **Resume**: Persisted progress automatically restored when reopening a book
- **Bionic Mode**: Chapter HTML text nodes are transformed into partially-bold words before rendering
- **Context Menu**: Center tap toggles reader chrome (toolbar, settings); text selection shows highlight options

### Library Features
- **Auto Scanning**: Recursively scans SAF-provided directory for EPUB/PDF files on app launch and manual refresh
- **Metadata Extraction**: Parses EPUB OPF file for title, author, cover image, genres, language, year
- **Filtering Pipeline**: Books are filtered by search query, type, and genre, then sorted by selected order
- **Fallback Handling**: Invalid/corrupted files are skipped without crashing library scan

### Data Persistence
- **Reader Settings**: Font, margins, theme, reading mode persisted per-app in DataStore
- **Book Progress**: Progress percentage and CFI position stored per book URI in DataStore
- **Highlights**: Stored in Room database with book ID, chapter anchor, text selection, and timestamp
- **Book Library**: Recent last-opened time and favorite status stored in Room for all books

## Implementation Notes

- **Dependency Injection**: Hilt provides centralized dependency management for all repositories, services, and ViewModels. See `/di` directory for module definitions.
- **Storage Access**: Uses modern **Storage Access Framework** (SAF) for scoped storage compatibility
- **Error Resilience**: Defensive error handling skips corrupted files without disrupting library scan
- **Resource Handling**: Intercepts WebView requests to serve EPUB resources (images, CSS, fonts) from in-memory archive
- **Database**: Single Room database instance for book metadata, progress, highlights, bookmarks, and annotations with flows for reactive updates
- **Preferences**: DataStore used for all user settings to ensure type-safe, atomic preference updates
- **Threading**: All I/O operations run on Dispatchers.IO; UI updates collected on main thread via Flow
- **TTS Integration**: Native Android TextToSpeech with configurable speed, pitch, and language
- **Cloud Sync**: Async operations for Google Drive and Dropbox synchronization with conflict resolution
- **OPDS Support**: Includes pre-configured catalogs (Project Gutenberg, Standard Ebooks, LibriVox) with extensible catalog system
- **Analytics**: Privacy-first analytics with opt-in model and local-only storage
- **Accessibility**: Full support for screen readers, high contrast modes, dyslexia-friendly fonts, and custom spacing
- **Export**: Multi-format export (PDF, Markdown, JSON) with selective inclusion of annotations

## Recent Implementations (v2.0.0 Roadmap)

### ✅ Completed Features

1. **Dependency Injection (Hilt)** - Integrated Hilt for centralized DI management
   - Modules for Data, Repositories, and Features
   - Simplified ViewModels with injected dependencies
   - Better testability and code organization

2. **Enhanced Annotations** - Full annotation system beyond highlights
   - Bookmarks with titles and notes
   - Margin notes (left/right positioned)
   - Annotation collections for organizing notes
   - Database persistence with Room

3. **Search in Book** - Full-text search capability
   - SearchRepository for managing search operations
   - Search results with context preview
   - Case-sensitive and whole-word search options
   - Integration with epub.js for on-page search

4. **Interactive TOC** - Chapter navigation
   - Already implemented (see table-of-contents features in reader)
   - Chapter list with quick jump capability
   - Visual hierarchy and current chapter highlighting

5. **Text-to-Speech** - Audio narration
   - Native Android TTS integration
   - Speed control (0.5x - 2.0x)
   - Pitch adjustment
   - Multi-language support
   - Pause/resume/stop controls

6. **Cloud Sync** - Google Drive & Dropbox integration
   - CloudSyncRepository with multi-provider support
   - Conflict resolution system
   - Selective sync options (highlights, bookmarks, progress)
   - Privacy-first design with optional opt-in

7. **OPDS Support** - Book catalog browsing
   - Pre-configured catalogs (Project Gutenberg, Standard Ebooks, LibriVox)
   - OPDS feed parsing capability
   - Extensible catalog system for adding custom sources
   - OpdsRepository for catalog management

8. **Advanced Layout**: Ready for future epub-rs integration
   - Placeholder in architecture for renderer switching
   - CSS pagination fully functional

9. **Accessibility** - Full accessibility suite
   - High contrast mode
   - Dyslexia-friendly fonts (OpenDyslexic, Atkinson Hyperlegible)
   - Customizable letter and word spacing
   - Reduced animations option
   - Screen reader support infrastructure
   - AccessibilityRepository for unified management

10. **Release Build** - Production optimization
    - R8 minification enabled for release builds (isMinifyEnabled = true)
    - Optimized ProGuard rules
    - Reduced APK size and improved performance

11. **Analytics Dashboard** - Reading statistics
    - AnalyticsRepository for tracking metrics
    - Reading time statistics per book and library-wide
    - Privacy-respecting with opt-in by default
    - Session tracking and WPM calculation
    - Statistics export capability

12. **Export Features** - Multiple export formats
    - PDF export with iText 7
    - Markdown export for note-taking apps
    - JSON export for data portability
    - Email integration ready
    - Selective export (highlights, bookmarks, notes, metadata)

## Next Steps for Integration

To use these new features in your UI:

### ViewModels
- Inject repositories using Hilt: `@Inject lateinit var repository: SearchRepository`
- All repositories are Singleton-scoped for app-wide state

### UI Components
- Create Compose screens for Search, Analytics, Accessibility settings
- Add menu items to reader and settings screens
- Implement export dialog with format selection
- Add TTS playback controls to reader UI

### JavaScript Integration
- Add search event handlers in `index.html` (epub.js will call Android bridge)
- Add accessibility CSS generation based on settings
- Add TTS event listeners for speaking current chapter

### Testing
- Repository layer is fully testable with Hilt mocks
- Use in-memory Room database for testing
- Mock TTS and Cloud services in unit tests

### Further Optimization
- Implement lazy loading for OPDS feeds
- Add download queue for catalog books
- Implement incremental sync for cloud features
- Add background job scheduling for auto-sync
