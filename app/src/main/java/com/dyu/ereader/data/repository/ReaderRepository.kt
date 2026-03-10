package com.dyu.ereader.data.repository

import com.dyu.ereader.data.database.BookDao
import com.dyu.ereader.data.database.HighlightDao
import com.dyu.ereader.data.database.HighlightEntity
import com.dyu.ereader.data.database.BookmarkDao
import com.dyu.ereader.data.database.BookmarkEntity
import com.dyu.ereader.data.database.MarginNoteDao
import com.dyu.ereader.data.database.MarginNoteEntity
import com.dyu.ereader.data.database.AnnotationCollectionDao
import com.dyu.ereader.data.database.AnnotationCollectionEntity
import com.dyu.ereader.data.model.ReaderSettings
import com.dyu.ereader.data.storage.ReaderPreferencesStore
import kotlinx.coroutines.flow.Flow

class ReaderRepository(
    private val preferences: ReaderPreferencesStore,
    private val highlightDao: HighlightDao,
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao,
    private val marginNoteDao: MarginNoteDao,
    private val collectionDao: AnnotationCollectionDao
) {

    val readerSettingsFlow: Flow<ReaderSettings> = preferences.readerSettingsFlow

    fun getBookProgress(bookUri: String): Flow<Float> = preferences.bookProgressFlow(bookUri)
    
    fun getBookCfi(bookUri: String): Flow<String?> = preferences.bookCfiFlow(bookUri)

    suspend fun saveProgress(bookUri: String, progress: Float, cfi: String? = null) {
        preferences.setBookProgress(bookUri, progress, cfi)
    }

    suspend fun updateLastOpened(bookUri: String) {
        val bookId = com.dyu.ereader.util.stableMd5(bookUri)
        bookDao.updateLastOpened(bookId, System.currentTimeMillis())
    }

    suspend fun saveReaderSettings(settings: ReaderSettings) {
        preferences.setReaderSettings(settings)
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
