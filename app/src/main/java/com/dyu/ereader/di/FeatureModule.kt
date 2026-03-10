package com.dyu.ereader.di

import android.content.Context
import com.dyu.ereader.data.database.BookDatabase
import com.dyu.ereader.data.repository.AccessibilityRepository
import com.dyu.ereader.data.repository.AnalyticsRepository
import com.dyu.ereader.data.repository.CloudSyncRepository
import com.dyu.ereader.data.repository.ExportRepository
import com.dyu.ereader.data.repository.BrowseRepository
import com.dyu.ereader.data.repository.SearchRepository
import com.dyu.ereader.data.repository.TextToSpeechRepository
import com.dyu.ereader.data.storage.ReaderPreferencesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeatureModule {

    @Singleton
    @Provides
    fun provideSearchRepository(
        @ApplicationContext context: Context
    ): SearchRepository {
        return SearchRepository(context)
    }

    @Singleton
    @Provides
    fun provideTextToSpeechRepository(
        @ApplicationContext context: Context
    ): TextToSpeechRepository {
        return TextToSpeechRepository(context)
    }

    @Singleton
    @Provides
    fun provideCloudSyncRepository(
        @ApplicationContext context: Context,
        database: BookDatabase,
        preferencesStore: ReaderPreferencesStore
    ): CloudSyncRepository {
        return CloudSyncRepository(context, database, preferencesStore)
    }

    @Singleton
    @Provides
    fun provideBrowseRepository(
        @ApplicationContext context: Context
    ): BrowseRepository {
        return BrowseRepository(context)
    }

    @Singleton
    @Provides
    fun provideAccessibilityRepository(
        @ApplicationContext context: Context
    ): AccessibilityRepository {
        return AccessibilityRepository(context)
    }

    @Singleton
    @Provides
    fun provideAnalyticsRepository(): AnalyticsRepository {
        return AnalyticsRepository()
    }

    @Singleton
    @Provides
    fun provideExportRepository(
        @ApplicationContext context: Context
    ): ExportRepository {
        return ExportRepository(context)
    }
}
