package com.dyu.ereader.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BookEntity::class,
        HighlightEntity::class,
        BookmarkEntity::class,
        MarginNoteEntity::class,
        AnnotationCollectionEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun highlightDao(): HighlightDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun marginNoteDao(): MarginNoteDao
    abstract fun annotationCollectionDao(): AnnotationCollectionDao
}
