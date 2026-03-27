package com.dyu.ereader.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY title ASC")
    suspend fun getAllBooksOnce(): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Query("DELETE FROM books")
    suspend fun clearAllBooks()

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :bookId")
    suspend fun updateFavorite(bookId: String, isFavorite: Boolean)

    @Query("UPDATE books SET lastOpened = :timestamp WHERE id = :bookId")
    suspend fun updateLastOpened(bookId: String, timestamp: Long)

    @Query("UPDATE books SET lastOpened = :timestamp WHERE uri = :uri")
    suspend fun updateLastOpenedByUri(uri: String, timestamp: Long)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)

    @Query("DELETE FROM books WHERE id NOT IN (:remainingIds)")
    suspend fun deleteMissingBooks(remainingIds: List<String>)
    
    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): BookEntity?

    @Query("SELECT * FROM books WHERE uri = :uri LIMIT 1")
    suspend fun getBookByUri(uri: String): BookEntity?
}
