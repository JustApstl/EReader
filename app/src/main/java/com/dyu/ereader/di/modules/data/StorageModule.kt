package com.dyu.ereader.di.modules.data

import android.content.Context
import com.dyu.ereader.data.local.prefs.ReaderPreferencesStore
import com.dyu.ereader.data.local.scanner.LibraryScanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

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
