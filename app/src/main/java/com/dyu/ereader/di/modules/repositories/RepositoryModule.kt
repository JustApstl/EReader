package com.dyu.ereader.di.modules.repositories

import android.content.Context
import com.dyu.ereader.data.local.db.BookDatabase
import com.dyu.ereader.data.local.prefs.ReaderPreferencesStore
import com.dyu.ereader.data.local.scanner.LibraryScanner
import com.dyu.ereader.data.repository.library.LibraryRepository
import com.dyu.ereader.data.repository.reader.ReaderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideLibraryRepository(
        @ApplicationContext context: Context,
        scanner: LibraryScanner,
        database: BookDatabase
    ): LibraryRepository {
        return LibraryRepository(context, scanner, database.bookDao())
    }

    @Singleton
    @Provides
    fun provideReaderRepository(
        preferences: ReaderPreferencesStore,
        database: BookDatabase
    ): ReaderRepository {
        return ReaderRepository(
            preferences = preferences,
            highlightDao = database.highlightDao(),
            bookDao = database.bookDao(),
            bookmarkDao = database.bookmarkDao(),
            marginNoteDao = database.marginNoteDao(),
            collectionDao = database.annotationCollectionDao()
        )
    }
}
