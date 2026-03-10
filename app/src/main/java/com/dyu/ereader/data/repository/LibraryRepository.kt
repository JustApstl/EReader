package com.dyu.ereader.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.dyu.ereader.data.database.BookDao
import com.dyu.ereader.data.database.toBookEntity
import com.dyu.ereader.data.database.toBookItem
import com.dyu.ereader.data.model.BookItem
import com.dyu.ereader.data.model.BookType
import com.dyu.ereader.data.storage.LibraryScanner
import com.dyu.ereader.util.EpubMetadataReader
import com.dyu.ereader.util.stableMd5
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class LibraryRepository(
    private val context: Context,
    private val scanner: LibraryScanner,
    private val bookDao: BookDao
) {

    fun getBooksFlow(): Flow<List<BookItem>> {
        return bookDao.getAllBooks().map { entities ->
            entities.map { it.toBookItem() }
        }
    }

    suspend fun toggleFavorite(bookId: String, isFavorite: Boolean) {
        bookDao.updateFavorite(bookId, isFavorite)
    }

    fun onLibraryAccessGranted(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    fun revokeLibraryAccess(uriString: String) {
        runCatching {
            val uri = uriString.toUri()
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    suspend fun scanBooks(treeUri: Uri) = withContext(Dispatchers.IO) {
        val files = scanner.scanTree(treeUri)
        val scannedIds = mutableListOf<String>()

        val booksToInsert = files.mapNotNull { file ->
            val id = stableMd5(file.uri.toString())
            scannedIds.add(id)
            
            val existing = bookDao.getBookById(id)
            existing?.copy(
                uri = file.uri.toString(),
                fileName = file.name
            )
                ?: runCatching {
                    val docFile = DocumentFile.fromSingleUri(context, file.uri)
                    val size = docFile?.length() ?: 0L

                    when (file.extension) {
                        "epub" -> {
                            EpubMetadataReader.readMetadata(
                                context = context,
                                uri = file.uri,
                                id = id,
                                fileName = file.name,
                                fileSize = size
                            )?.toBookEntity()
                        }
                        "pdf" -> {
                            BookItem(
                                id = id,
                                uri = file.uri.toString(),
                                fileName = file.name,
                                title = file.name.removeSuffix(".pdf"),
                                author = "Unknown",
                                coverImage = null,
                                type = BookType.PDF,
                                fileSize = size,
                                dateAdded = System.currentTimeMillis()
                            ).toBookEntity()
                        }
                        else -> null
                    }
                }.getOrNull()
        }

        bookDao.insertBooks(booksToInsert)
        bookDao.deleteMissingBooks(scannedIds)
    }

    suspend fun importBook(file: File) = withContext(Dispatchers.IO) {
        val id = stableMd5(file.absolutePath)
        
        val docUri = file.toUri()
        val size = file.length()
        
        val bookEntity = when (file.extension.lowercase()) {
            "epub" -> {
                try {
                    val bookItem = EpubMetadataReader.readMetadata(
                        context = context,
                        uri = docUri,
                        id = id,
                        fileName = file.name,
                        fileSize = size
                    )
                    bookItem?.toBookEntity()
                } catch (e: Exception) {
                    null
                }
            }
            "pdf" -> {
                BookItem(
                    id = id,
                    uri = docUri.toString(),
                    fileName = file.name,
                    title = file.name.removeSuffix(".pdf"),
                    author = "Downloaded",
                    coverImage = null,
                    type = BookType.PDF,
                    fileSize = size,
                    dateAdded = System.currentTimeMillis()
                ).toBookEntity()
            }
            else -> null
        }

        bookEntity?.let {
            bookDao.insertBooks(listOf(it))
            Result.success(Unit)
        } ?: Result.failure(Exception("Unable to read book metadata"))
    }
}
