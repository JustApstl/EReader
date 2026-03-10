package com.dyu.ereader.di

import android.content.Context
import com.dyu.ereader.data.database.BookDatabase
import com.dyu.ereader.data.repository.LibraryRepository
import com.dyu.ereader.data.repository.ReaderRepository
import com.dyu.ereader.data.storage.LibraryScanner
import com.dyu.ereader.data.storage.ReaderPreferencesStore
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
