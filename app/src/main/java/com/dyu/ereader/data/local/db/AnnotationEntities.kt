package com.dyu.ereader.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val chapterAnchor: String,
    val cfi: String, // EPUB Canonical Fragment Identifier
    val title: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "margin_notes")
data class MarginNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val chapterAnchor: String,
    val cfi: String,
    val position: String, // LEFT, RIGHT
    val content: String,
    val color: String = "#FFFF00",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "annotation_collections")
data class AnnotationCollectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val name: String,
    val description: String? = null,
    val isPublic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val chapterAnchor: String,
    val selectionJson: String, // Store serialized selection range or unique path
    val selectedText: String,
    val color: String, // Hex color
    val createdAt: Long = System.currentTimeMillis()
)
