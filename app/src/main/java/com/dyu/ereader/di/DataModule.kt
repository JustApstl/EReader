package com.dyu.ereader.di

import android.content.Context
import androidx.room.Room
import com.dyu.ereader.data.database.BookDatabase
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
object DataModule {

    @Singleton
    @Provides
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): BookDatabase {
        return Room.databaseBuilder(
            context,
            BookDatabase::class.java,
            "ereader_database"
        ).build()
    }

    @Singleton
    @Provides
    fun provideReaderPreferencesStore(
        @ApplicationContext context: Context
    ): ReaderPreferencesStore {
        return ReaderPreferencesStore(context)
    }

    @Singleton
    @Provides
    fun provideLibraryScanner(
        @ApplicationContext context: Context
    ): LibraryScanner {
        return LibraryScanner(context)
    }
}
