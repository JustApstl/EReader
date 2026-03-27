package com.dyu.ereader.di.modules.data

import android.content.Context
import androidx.room.Room
import com.dyu.ereader.data.local.db.BookDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): BookDatabase {
        return Room.databaseBuilder(
            context,
            BookDatabase::class.java,
            "ereader_database"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
}
