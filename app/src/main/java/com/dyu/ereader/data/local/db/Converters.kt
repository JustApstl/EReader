package com.dyu.ereader.data.local.db

import androidx.room.TypeConverter
import com.dyu.ereader.data.model.library.BookType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromBookType(value: BookType): String {
        return value.name
    }

    @TypeConverter
    fun toBookType(value: String): BookType {
        val normalized = value.trim().uppercase()
        return when (normalized) {
            "EPUB2" -> BookType.EPUB
            "EPUB3" -> BookType.EPUB3
            "PDF" -> BookType.PDF
            "AZW3" -> BookType.AZW3
            "MOBI" -> BookType.MOBI
            "CBZ" -> BookType.CBZ
            "CBR" -> BookType.CBR
            else -> BookType.entries.firstOrNull { it.name == normalized } ?: BookType.EPUB
        }
    }
}
