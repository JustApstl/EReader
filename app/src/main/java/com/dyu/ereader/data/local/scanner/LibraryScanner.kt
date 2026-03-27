package com.dyu.ereader.data.local.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.dyu.ereader.data.format.BookFormatRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScannedFile(
    val uri: Uri,
    val name: String,
    val extension: String
)

class LibraryScanner(private val context: Context) {

    suspend fun scanTree(treeUri: Uri): List<ScannedFile> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        if (!root.exists()) return@withContext emptyList()

        val results = mutableListOf<ScannedFile>()
        val queue = ArrayDeque<DocumentFile>()
        queue.add(root)

        val supportedExtensions = BookFormatRegistry.supportedExtensions

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val children = runCatching { current.listFiles() }.getOrElse { emptyArray() }

            for (child in children) {
                if (child.isDirectory) {
                    queue.add(child)
                    continue
                }

                if (child.isFile) {
                    val name = child.name ?: continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in supportedExtensions) {
                        results.add(
                            ScannedFile(
                                uri = child.uri,
                                name = name,
                                extension = ext
                            )
                        )
                    }
                }
            }
        }

        results
    }
}
