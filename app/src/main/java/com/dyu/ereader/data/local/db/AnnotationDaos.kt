package com.dyu.ereader.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getBookmarksForBook(bookId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarksOnce(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getBookmarkById(id: Long): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Query("DELETE FROM bookmarks")
    suspend fun clearAllBookmarks()

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteBookmarksForBook(bookId: String)
}

@Dao
interface MarginNoteDao {
    @Query("SELECT * FROM margin_notes WHERE bookId = :bookId AND chapterAnchor = :chapterAnchor")
    fun getMarginNotesForChapter(bookId: String, chapterAnchor: String): Flow<List<MarginNoteEntity>>

    @Query("SELECT * FROM margin_notes WHERE bookId = :bookId")
    fun getMarginalNotesForBook(bookId: String): Flow<List<MarginNoteEntity>>

    @Query("SELECT * FROM margin_notes")
    suspend fun getAllMarginNotesOnce(): List<MarginNoteEntity>

    @Query("SELECT * FROM margin_notes WHERE id = :id")
    suspend fun getMarginNoteById(id: Long): MarginNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarginNote(note: MarginNoteEntity): Long

    @Query("DELETE FROM margin_notes WHERE bookId = :bookId AND cfi = :cfi")
    suspend fun deleteMarginNotesByCfi(bookId: String, cfi: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarginNotes(notes: List<MarginNoteEntity>)

    @Query("DELETE FROM margin_notes")
    suspend fun clearAllMarginNotes()

    @Update
    suspend fun updateMarginNote(note: MarginNoteEntity)

    @Delete
    suspend fun deleteMarginNote(note: MarginNoteEntity)

    @Query("DELETE FROM margin_notes WHERE bookId = :bookId")
    suspend fun deleteMarginNotesForBook(bookId: String)
}

@Dao
interface AnnotationCollectionDao {
    @Query("SELECT * FROM annotation_collections WHERE bookId = :bookId")
    fun getCollectionsForBook(bookId: String): Flow<List<AnnotationCollectionEntity>>

    @Query("SELECT * FROM annotation_collections")
    suspend fun getAllCollectionsOnce(): List<AnnotationCollectionEntity>

    @Query("SELECT * FROM annotation_collections WHERE id = :id")
    suspend fun getCollectionById(id: Long): AnnotationCollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: AnnotationCollectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<AnnotationCollectionEntity>)

    @Query("DELETE FROM annotation_collections")
    suspend fun clearAllCollections()

    @Update
    suspend fun updateCollection(collection: AnnotationCollectionEntity)

    @Delete
    suspend fun deleteCollection(collection: AnnotationCollectionEntity)
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE bookId = :bookId")
    fun getHighlightsForBook(bookId: String): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights")
    suspend fun getAllHighlightsOnce(): List<HighlightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlights(highlights: List<HighlightEntity>)

    @Query("DELETE FROM highlights")
    suspend fun clearAllHighlights()

    @Query("DELETE FROM highlights WHERE bookId = :bookId AND selectionJson = :selectionJson")
    suspend fun deleteHighlightsBySelection(bookId: String, selectionJson: String)

    @Query("DELETE FROM highlights WHERE id = :highlightId")
    suspend fun deleteHighlight(highlightId: Long)

    @Query("DELETE FROM highlights WHERE bookId = :bookId")
    suspend fun clearHighlightsForBook(bookId: String)
}
