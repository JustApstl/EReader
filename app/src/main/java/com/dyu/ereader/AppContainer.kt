package com.dyu.ereader

import android.content.Context
import androidx.room.Room
import com.dyu.ereader.data.database.BookDatabase
import com.dyu.ereader.data.repository.LibraryRepository
import com.dyu.ereader.data.repository.ReaderRepository
import com.dyu.ereader.data.storage.LibraryScanner
import com.dyu.ereader.data.storage.ReaderPreferencesStore

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: BookDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            BookDatabase::class.java,
            "ebook_reader_db"
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }

    val preferencesStore = ReaderPreferencesStore(appContext)
    private val scanner = LibraryScanner(appContext)

    val libraryRepository = LibraryRepository(
        context = appContext,
        scanner = scanner,
        bookDao = database.bookDao()
    )

    val readerRepository = ReaderRepository(
        preferences = preferencesStore,
        highlightDao = database.highlightDao(),
        bookDao = database.bookDao(),
        bookmarkDao = database.bookmarkDao(),
        marginNoteDao = database.marginNoteDao(),
        collectionDao = database.annotationCollectionDao()
    )
}
