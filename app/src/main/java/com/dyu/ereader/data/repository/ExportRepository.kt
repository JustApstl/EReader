package com.dyu.ereader.data.repository

import android.content.Context
import android.net.Uri
import com.dyu.ereader.data.model.ExportData
import com.dyu.ereader.data.model.ExportFormat
import com.dyu.ereader.data.model.ExportOptions
import com.google.gson.Gson
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document as PdfLayout
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.io.font.constants.StandardFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

class ExportRepository(
    private val context: Context
) {
    private val gson = Gson()

    suspend fun exportData(data: ExportData, options: ExportOptions): Uri? = withContext(Dispatchers.IO) {
        try {
            val fileName = "${data.bookTitle}_${System.currentTimeMillis()}"
            when (options.format) {
                ExportFormat.PDF -> exportToPdf(data, fileName)
                ExportFormat.MARKDOWN -> exportToMarkdown(data, fileName)
                ExportFormat.JSON -> exportToJson(data, fileName)
                ExportFormat.EMAIL -> null // Handled by EmailRepository
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun exportToPdf(data: ExportData, fileName: String): Uri? {
        return try {
            val file = File(context.cacheDir, "$fileName.pdf")
            val writer = PdfWriter(file)
            val pdfDocument = PdfDocument(writer)
            val document = PdfLayout(pdfDocument)
            val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)

            // Add title and metadata
            document.add(Paragraph("${data.bookTitle} by ${data.bookAuthor}").setFont(boldFont))
            document.add(Paragraph("Exported on: ${java.time.LocalDate.now()}"))
            document.add(Paragraph(""))

            // Add highlights
            if (data.highlights.isNotEmpty()) {
                document.add(Paragraph("Highlights:").setFont(boldFont))
                data.highlights.forEach { hl ->
                    document.add(Paragraph("${hl.chapter}: ${hl.text}"))
                    document.add(Paragraph(""))
                }
            }

            // Add bookmarks
            if (data.bookmarks.isNotEmpty()) {
                document.add(Paragraph("Bookmarks:").setFont(boldFont))
                data.bookmarks.forEach { bm ->
                    document.add(Paragraph("${bm.chapter}: ${bm.title}"))
                    if (bm.note != null) document.add(Paragraph("Note: ${bm.note}"))
                    document.add(Paragraph(""))
                }
            }

            document.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    private fun exportToMarkdown(data: ExportData, fileName: String): Uri? {
        return try {
            val file = File(context.cacheDir, "$fileName.md")
            val content = buildString {
                appendLine("# ${data.bookTitle}")
                appendLine("**By:** ${data.bookAuthor}")
                appendLine("**Exported:** ${java.time.LocalDate.now()}")
                appendLine()

                if (data.highlights.isNotEmpty()) {
                    appendLine("## Highlights")
                    data.highlights.forEach { hl ->
                        appendLine("- **${hl.chapter}:** ${hl.text}")
                        appendLine()
                    }
                }

                if (data.bookmarks.isNotEmpty()) {
                    appendLine("## Bookmarks")
                    data.bookmarks.forEach { bm ->
                        appendLine("- **${bm.chapter}:** ${bm.title}")
                        if (bm.note != null) appendLine("  Note: ${bm.note}")
                        appendLine()
                    }
                }

                if (data.notes.isNotEmpty()) {
                    appendLine("## Notes")
                    data.notes.forEach { note ->
                        appendLine("- **${note.chapter}:** ${note.content}")
                        appendLine()
                    }
                }
            }
            FileWriter(file).use { it.write(content) }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    private fun exportToJson(data: ExportData, fileName: String): Uri? {
        return try {
            val file = File(context.cacheDir, "$fileName.json")
            val json = gson.toJson(data)
            FileWriter(file).use { it.write(json) }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }
}
