package com.dyu.ereader.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val uri: String,
    val fileName: String,
    val title: String,
    val author: String,
    val coverImage: ByteArray?,
    val isFavorite: Boolean = false,
    val lastOpened: Long = 0,
    val dateAdded: Long = 0,
    val type: BookType = BookType.EPUB,
    val description: String? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val isbn: String? = null,
    val language: String? = null,
    val fileSize: Long = 0L,
    val year: String? = null,
    val genres: List<String> = emptyList(),
    val countries: List<String> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BookEntity

        if (isFavorite != other.isFavorite) return false
        if (lastOpened != other.lastOpened) return false
        if (dateAdded != other.dateAdded) return false
        if (fileSize != other.fileSize) return false
        if (id != other.id) return false
        if (uri != other.uri) return false
        if (fileName != other.fileName) return false
        if (title != other.title) return false
        if (author != other.author) return false
        if (!coverImage.contentEquals(other.coverImage)) return false
        if (type != other.type) return false
        if (description != other.description) return false
        if (publisher != other.publisher) return false
        if (publishedDate != other.publishedDate) return false
        if (isbn != other.isbn) return false
        if (language != other.language) return false
        if (year != other.year) return false
        if (genres != other.genres) return false
        if (countries != other.countries) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isFavorite.hashCode()
        result = 31 * result + lastOpened.hashCode()
        result = 31 * result + dateAdded.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + (coverImage?.contentHashCode() ?: 0)
        result = 31 * result + type.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (publisher?.hashCode() ?: 0)
        result = 31 * result + (publishedDate?.hashCode() ?: 0)
        result = 31 * result + (isbn?.hashCode() ?: 0)
        result = 31 * result + (language?.hashCode() ?: 0)
        result = 31 * result + (year?.hashCode() ?: 0)
        result = 31 * result + genres.hashCode()
        result = 31 * result + countries.hashCode()
        return result
    }
}

fun BookEntity.toBookItem(): BookItem {
    return BookItem(
        id = id,
        uri = uri,
        fileName = fileName,
        title = title,
        author = author,
        coverImage = coverImage,
        isFavorite = isFavorite,
        lastOpened = lastOpened,
        dateAdded = dateAdded,
        type = type,
        description = description,
        publisher = publisher,
        publishedDate = publishedDate,
        isbn = isbn,
        language = language,
        fileSize = fileSize,
        year = year,
        genres = genres,
        countries = countries
    )
}

fun BookItem.toBookEntity(): BookEntity {
    return BookEntity(
        id = id,
        uri = uri,
        fileName = fileName,
        title = title,
        author = author,
        coverImage = coverImage,
        isFavorite = isFavorite,
        lastOpened = lastOpened,
        dateAdded = dateAdded,
        type = type,
        description = description,
        publisher = publisher,
        publishedDate = publishedDate,
        isbn = isbn,
        language = language,
        fileSize = fileSize,
        year = year,
        genres = genres,
        countries = countries
    )
}
