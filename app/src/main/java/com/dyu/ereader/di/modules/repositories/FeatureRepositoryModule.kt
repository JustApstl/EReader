package com.dyu.ereader.di.modules.repositories

import android.content.Context
import com.dyu.ereader.data.local.db.BookDatabase
import com.dyu.ereader.data.local.prefs.ReaderPreferencesStore
import com.dyu.ereader.data.repository.accessibility.AccessibilityRepository
import com.dyu.ereader.data.repository.analytics.AnalyticsRepository
import com.dyu.ereader.data.repository.browse.BrowseRepository
import com.dyu.ereader.data.repository.cloud.CloudSyncRepository
import com.dyu.ereader.data.repository.export.ExportRepository
import com.dyu.ereader.data.repository.mobi.MobiConversionRepository
import com.dyu.ereader.data.repository.search.SearchRepository
import com.dyu.ereader.data.repository.tts.TextToSpeechRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeatureRepositoryModule {

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
        preferencesStore: ReaderPreferencesStore,
        analyticsRepository: AnalyticsRepository
    ): CloudSyncRepository {
        return CloudSyncRepository(context, database, preferencesStore, analyticsRepository)
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

    @Singleton
    @Provides
    fun provideMobiConversionRepository(
        @ApplicationContext context: Context
    ): MobiConversionRepository {
        return MobiConversionRepository(context)
    }
}
