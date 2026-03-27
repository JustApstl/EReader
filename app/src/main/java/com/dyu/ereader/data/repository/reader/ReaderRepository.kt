package com.dyu.ereader.data.repository.reader

import com.dyu.ereader.data.local.db.BookEntity
import com.dyu.ereader.data.local.db.BookDao
import com.dyu.ereader.data.local.db.HighlightDao
import com.dyu.ereader.data.local.db.HighlightEntity
import com.dyu.ereader.data.local.db.BookmarkDao
import com.dyu.ereader.data.local.db.BookmarkEntity
import com.dyu.ereader.data.local.db.MarginNoteDao
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.data.local.db.AnnotationCollectionDao
import com.dyu.ereader.data.local.db.AnnotationCollectionEntity
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.local.prefs.ReaderPreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ReaderRepository(
    private val preferences: ReaderPreferencesStore,
    private val highlightDao: HighlightDao,
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao,
    private val marginNoteDao: MarginNoteDao,
    private val collectionDao: AnnotationCollectionDao
) {
    val readerOnboardingSeenFlow: Flow<Boolean> = preferences.readerOnboardingSeenFlow

    fun readerSettingsFlow(bookId: String): Flow<ReaderSettings> {
        return combine(
            preferences.readerSettingsFlow,
            preferences.perBookReaderSettingsFlow(bookId)
        ) { global, perBook ->
            val resolved = perBook ?: global
            resolved.copy(navBarStyle = global.navBarStyle)
        }
    }

    suspend fun readResolvedReaderSettings(bookId: String): ReaderSettings {
        return preferences.readResolvedReaderSettings(bookId)
    }

    fun getBookProgress(bookUri: String): Flow<Float> = preferences.bookProgressFlow(bookUri)
    
    fun getBookCfi(bookUri: String): Flow<String?> = preferences.bookCfiFlow(bookUri)

    suspend fun saveProgress(bookUri: String, progress: Float, cfi: String? = null) {
        preferences.setBookProgress(bookUri, progress, cfi)
    }

    suspend fun updateLastOpened(bookUri: String) {
        val bookId = com.dyu.ereader.core.crypto.stableMd5(bookUri)
        val now = System.currentTimeMillis()
        bookDao.updateLastOpened(bookId, now)
        bookDao.updateLastOpenedByUri(bookUri, now)
    }

    suspend fun saveReaderSettings(bookId: String, settings: ReaderSettings) {
        preferences.setPerBookReaderSettings(bookId, settings)
    }

    suspend fun resetReaderSettings(bookId: String) {
        preferences.setPerBookReaderSettings(bookId, null)
    }

    suspend fun clearNewDownloadById(bookId: String) {
        preferences.clearNewDownload(bookId)
    }

    suspend fun setReaderOnboardingSeen(seen: Boolean) {
        preferences.setReaderOnboardingSeen(seen)
    }

    suspend fun getBookMetadata(bookId: String, bookUri: String): BookEntity? {
        return bookDao.getBookById(bookId) ?: bookDao.getBookByUri(bookUri)
    }

    // Highlights
    fun getHighlights(bookId: String): Flow<List<HighlightEntity>> {
        return highlightDao.getHighlightsForBook(bookId)
    }

    suspend fun addHighlight(highlight: HighlightEntity) {
        highlightDao.insertHighlight(highlight)
    }

    suspend fun removeHighlightsBySelection(bookId: String, selectionJson: String) {
        highlightDao.deleteHighlightsBySelection(bookId, selectionJson)
    }

    suspend fun removeHighlight(highlightId: Long) {
        highlightDao.deleteHighlight(highlightId)
    }

    // Bookmarks
    fun getBookmarks(bookId: String): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getBookmarksForBook(bookId)
    }

    suspend fun addBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    // Margin Notes
    fun getMarginNotes(bookId: String): Flow<List<MarginNoteEntity>> {
        return marginNoteDao.getMarginalNotesForBook(bookId)
    }

    suspend fun addMarginNote(note: MarginNoteEntity) {
        marginNoteDao.insertMarginNote(note)
    }

    suspend fun removeMarginNotesByCfi(bookId: String, cfi: String) {
        marginNoteDao.deleteMarginNotesByCfi(bookId, cfi)
    }

    suspend fun deleteMarginNote(note: MarginNoteEntity) {
        marginNoteDao.deleteMarginNote(note)
    }

    // Annotation Collections
    fun getCollections(bookId: String): Flow<List<AnnotationCollectionEntity>> {
        return collectionDao.getCollectionsForBook(bookId)
    }

    suspend fun addCollection(collection: AnnotationCollectionEntity) {
        collectionDao.insertCollection(collection)
    }
}
