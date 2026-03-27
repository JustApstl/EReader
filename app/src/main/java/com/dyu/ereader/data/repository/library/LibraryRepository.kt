package com.dyu.ereader.data.repository.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.dyu.ereader.data.local.db.BookDao
import com.dyu.ereader.data.local.db.toBookEntity
import com.dyu.ereader.data.local.db.toBookItem
import com.dyu.ereader.data.format.BookFormatRegistry
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.local.scanner.LibraryScanner
import com.dyu.ereader.core.crypto.stableMd5
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
            val docFile = DocumentFile.fromSingleUri(context, file.uri)
            val size = docFile?.length() ?: 0L
            val extension = file.extension
            val handler = BookFormatRegistry.handlerForExtension(extension)
            val existingUpdated = existing?.copy(
                uri = file.uri.toString(),
                fileName = file.name,
                fileSize = size
            )

            if (existingUpdated != null) {
                val existingItem = existingUpdated.toBookItem()
                if (handler != null && handler.shouldRefreshMetadata(existingItem)) {
                    val refreshed = handler.readMetadata(
                        context = context,
                        uri = file.uri,
                        id = id,
                        fileName = file.name,
                        fileSize = size,
                        fallbackAuthor = existingItem.author.ifBlank { "Unknown" },
                        dateAdded = existingItem.dateAdded
                    )
                    return@mapNotNull if (refreshed != null) {
                        handler.mergeWithExisting(existingItem, refreshed).toBookEntity()
                    } else {
                        existingUpdated
                    }
                }
                existingUpdated
            } else {
                if (handler == null) {
                    return@mapNotNull null
                }
                val item = runCatching {
                    handler.readMetadata(
                        context = context,
                        uri = file.uri,
                        id = id,
                        fileName = file.name,
                        fileSize = size,
                        fallbackAuthor = "Unknown",
                        dateAdded = System.currentTimeMillis()
                    )
                }.getOrNull()
                item?.toBookEntity()
            }
        }

        bookDao.insertBooks(booksToInsert)

        val existingBooks = bookDao.getAllBooksOnce()
        val retainedIds = existingBooks.mapNotNull { entity ->
            val uri = runCatching { Uri.parse(entity.uri) }.getOrNull() ?: return@mapNotNull null
            if (uri.scheme != "file") return@mapNotNull null
            val path = uri.path ?: return@mapNotNull null
            if (File(path).exists()) entity.id else null
        }

        val remainingIds = (scannedIds + retainedIds).distinct()
        bookDao.deleteMissingBooks(remainingIds)
    }

    suspend fun importBook(file: File): Result<BookItem> = withContext(Dispatchers.IO) {
        val docUri = file.toUri()
        val id = stableMd5(docUri.toString())
        val legacyId = stableMd5(file.absolutePath)
        if (legacyId != id) {
            runCatching { bookDao.deleteBook(legacyId) }
        }
        val size = file.length()
        val handler = BookFormatRegistry.handlerForExtension(file.extension.lowercase())
        val bookItem = handler?.readMetadata(
            context = context,
            uri = docUri,
            id = id,
            fileName = file.name,
            fileSize = size,
            fallbackAuthor = "Downloaded",
            dateAdded = System.currentTimeMillis()
        )

        val bookEntity = bookItem?.toBookEntity()
        if (bookEntity != null) {
            bookDao.insertBooks(listOf(bookEntity))
            Result.success(bookItem)
        } else {
            Result.failure(Exception("Unable to read book metadata"))
        }
    }

    suspend fun importBookToLibrary(file: File, libraryTreeUri: String?): Result<BookItem> = withContext(Dispatchers.IO) {
        if (libraryTreeUri.isNullOrBlank()) {
            return@withContext importBook(file)
        }

        val treeUri = runCatching { Uri.parse(libraryTreeUri) }.getOrNull()
            ?: return@withContext importBook(file)
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext importBook(file)
        if (!root.canWrite()) {
            return@withContext importBook(file)
        }

        val extension = file.extension.lowercase()
        val mimeType = BookFormatRegistry.resolveMimeType(extension)

        val targetName = file.name
        val targetFile = root.createFile(mimeType, targetName) ?: return@withContext importBook(file)

        val output = context.contentResolver.openOutputStream(targetFile.uri)
            ?: return@withContext importBook(file)
        output.use { out ->
            file.inputStream().use { input ->
                input.copyTo(out)
            }
        }

        val targetUri = targetFile.uri
        val targetSize = targetFile.length()
        val targetFileName = targetFile.name ?: file.name
        val id = stableMd5(targetUri.toString())

        val handler = BookFormatRegistry.handlerForExtension(extension)
        val bookItem = handler?.readMetadata(
            context = context,
            uri = targetUri,
            id = id,
            fileName = targetFileName,
            fileSize = targetSize,
            fallbackAuthor = "Downloaded",
            dateAdded = System.currentTimeMillis()
        )

        val bookEntity = bookItem?.toBookEntity()
        if (bookEntity == null) {
            runCatching { targetFile.delete() }
            return@withContext Result.failure(Exception("Unable to read book metadata"))
        }

        bookDao.insertBooks(listOf(bookEntity))
        file.delete()
        Result.success(bookItem)
    }

    suspend fun deleteBook(book: BookItem): Result<Unit> = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(book.uri) }.getOrNull()
        val deleted = when (uri?.scheme) {
            "content" -> DocumentFile.fromSingleUri(context, uri)?.delete() ?: false
            "file" -> {
                val path = uri.path ?: return@withContext Result.failure(Exception("Unable to delete book file"))
                val file = File(path)
                if (!file.exists()) true else file.delete()
            }
            else -> false
        }

        if (!deleted) {
            return@withContext Result.failure(Exception("Unable to delete book file"))
        }

        bookDao.deleteBook(book.id)
        Result.success(Unit)
    }
}
