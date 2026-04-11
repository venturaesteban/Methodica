package com.methodica.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.methodica.app.core.ai.AiDocumentProcessingPolicy
import com.methodica.app.data.local.dao.MaterialDao
import com.methodica.app.data.local.entity.toDomain
import com.methodica.app.data.local.entity.toEntity
import com.methodica.app.domain.model.Material
import com.methodica.app.domain.model.MaterialType
import com.methodica.app.domain.repository.MaterialRepository
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.LinkedHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MaterialRepositoryImpl(
    private val dao: MaterialDao,
    @ApplicationContext private val context: Context
) : MaterialRepository {

    override fun observeAllMaterials(): Flow<List<Material>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getMaterial(id: Long): Material? =
        dao.getById(id)?.toDomain()

    override suspend fun saveMaterial(material: Material): Long =
        dao.upsert(material.toEntity()).also {
            invalidateSummaryCache(material)
        }

    override suspend fun deleteMaterial(material: Material) =
        dao.delete(material.toEntity()).also {
            invalidateSummaryCache(material)
        }

    override suspend fun buildAiResourceSummary(material: Material, maxChars: Int): String? {
        getCachedSummary(material)?.let { cached ->
            return cached.take(maxChars.coerceAtLeast(256))
        }

        val summary = when (material.type) {
            MaterialType.FILE_URI -> summarizeLocalFile(material, maxChars)
            MaterialType.WEB_LINK -> summarizeWebLink(material)
            MaterialType.VIDEO_LINK -> summarizeVideoLink(material)
        }
        cacheSummary(material, summary)
        return summary
    }


    override suspend fun buildAiIndexableContent(material: Material, maxChars: Int): String? {
        return when (material.type) {
            MaterialType.FILE_URI -> extractLocalFileText(material, maxChars)
            MaterialType.WEB_LINK -> summarizeWebLink(material)
            MaterialType.VIDEO_LINK -> summarizeVideoLink(material)
        }
    }
    private fun summarizeLocalFile(material: Material, maxChars: Int): String? {
        val uri = runCatching { Uri.parse(material.uri) }.getOrNull() ?: return null
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty()
        val (displayName, sizeBytes) = queryOpenableMetadata(uri)
        val extension = displayName.substringAfterLast('.', "").lowercase()

        if (mime.contains("pdf", ignoreCase = true) || extension == "pdf") {
            val pageCount = extractPdfPageCount(uri)
            val pdfSnippet = extractPdfTextSnippet(uri, maxChars = maxChars)
                ?: extractPdfTextWithOcr(uri, maxChars = maxChars)
            return buildString {
                append("Recurso local PDF")
                if (displayName.isNotBlank()) append(" ($displayName)")
                if (sizeBytes != null && sizeBytes > 0) append(", tamano=${sizeBytes / 1024}KB")
                if (pageCount != null) append(", paginas=$pageCount")
                if (!pdfSnippet.isNullOrBlank()) {
                    append("\nExtracto PDF:\n")
                    append(pdfSnippet)
                } else {
                    append(". Nota: no se pudo extraer texto del PDF (embebido u OCR).")
                }
            }
        }

        val isTextLike = mime.startsWith("text/") || extension in TEXT_EXTENSIONS
        if (isTextLike) {
            val text = readTextFromUri(uri, maxChars)
            if (!text.isNullOrBlank()) {
                return buildString {
                    append("Recurso local de texto")
                    if (displayName.isNotBlank()) append(" ($displayName)")
                    if (sizeBytes != null && sizeBytes > 0) append(", tamano=${sizeBytes / 1024}KB")
                    append("\nExtracto:\n")
                    append(text)
                }
            }
        }

        return buildString {
            append("Recurso local no textual")
            if (displayName.isNotBlank()) append(" ($displayName)")
            if (mime.isNotBlank()) append(", mime=$mime")
            if (sizeBytes != null && sizeBytes > 0) append(", tamano=${sizeBytes / 1024}KB")
        }
    }


    private fun extractLocalFileText(material: Material, maxChars: Int): String? {
        val uri = runCatching { Uri.parse(material.uri) }.getOrNull() ?: return null
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty()
        val (displayName, _) = queryOpenableMetadata(uri)
        val extension = displayName.substringAfterLast('.', "").lowercase()
        if (mime.contains("pdf", ignoreCase = true) || extension == "pdf") {
            return extractPdfTextSnippet(uri, maxChars = maxChars)
                ?: extractPdfTextWithOcr(uri, maxChars = maxChars)
        }
        val isTextLike = mime.startsWith("text/") || extension in TEXT_EXTENSIONS
        if (isTextLike) return readTextFromUri(uri, maxChars)
        return null
    }

    private fun summarizeWebLink(material: Material): String {
        val uri = runCatching { Uri.parse(material.uri) }.getOrNull()
        val host = uri?.host.orEmpty()
        return if (host.isBlank()) {
            "Enlace web asociado: ${material.uri}"
        } else {
            "Enlace web asociado en host $host: ${material.uri}"
        }
    }

    private fun summarizeVideoLink(material: Material): String {
        val uri = runCatching { Uri.parse(material.uri) }.getOrNull()
        val host = uri?.host.orEmpty()
        return if (host.isBlank()) {
            "Video asociado: ${material.uri}"
        } else {
            "Video asociado en host $host: ${material.uri}"
        }
    }

    private fun queryOpenableMetadata(uri: Uri): Pair<String, Long?> {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use "" to null
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
                    val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                    name to size
                } ?: ("" to null)
        }.getOrElse { "" to null }
    }

    private fun extractPdfPageCount(uri: Uri): Int? {
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                PdfRenderer(fd).use { renderer ->
                    renderer.pageCount
                }
            }
        }.getOrNull()
    }

    private fun readTextFromUri(uri: Uri, maxChars: Int): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                readLimitedText(input, maxChars)
            }
        }.getOrNull()
    }

    private fun extractPdfTextSnippet(uri: Uri, maxChars: Int): String? {
        return runCatching {
            ensurePdfBoxInitialized()
            context.contentResolver.openInputStream(uri)?.use { input ->
                PDDocument.load(input).use { document ->
                    val pageCount = document.numberOfPages.coerceAtLeast(1)
                    val stripper = PDFTextStripper().apply {
                        startPage = 1
                        endPage = pageCount.coerceAtMost(AiDocumentProcessingPolicy.MAX_PDF_PAGES_TEXT)
                    }
                    cleanTextForAi(stripper.getText(document), maxChars)
                        .ifBlank { null }
                }
            }
        }.getOrNull()
    }

    private fun extractPdfTextWithOcr(uri: Uri, maxChars: Int): String? {
        return runCatching {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val merged = StringBuilder()
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                PdfRenderer(fd).use { renderer ->
                    val pagesToScan = minOf(renderer.pageCount, AiDocumentProcessingPolicy.MAX_PDF_PAGES_OCR)
                    for (pageIndex in 0 until pagesToScan) {
                        if (merged.length >= maxChars) break
                        renderer.openPage(pageIndex).use { page ->
                            val bitmap = renderPageBitmap(page)
                            val image = InputImage.fromBitmap(bitmap, 0)
                            val recognized = Tasks.await(recognizer.process(image)).text
                            if (recognized.isNotBlank()) {
                                merged.append(recognized).append(' ')
                            }
                            bitmap.recycle()
                        }
                    }
                }
            }
            recognizer.close()
            cleanTextForAi(merged.toString(), maxChars)
                .ifBlank { null }
        }.getOrNull()
    }

    private fun renderPageBitmap(page: PdfRenderer.Page): Bitmap {
        val width = (page.width * AiDocumentProcessingPolicy.OCR_RENDER_SCALE).toInt().coerceAtLeast(1)
        val height = (page.height * AiDocumentProcessingPolicy.OCR_RENDER_SCALE).toInt().coerceAtLeast(1)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }
    }

    private fun ensurePdfBoxInitialized() {
        if (pdfBoxInitialized.get()) return
        synchronized(pdfBoxInitialized) {
            if (!pdfBoxInitialized.get()) {
                PDFBoxResourceLoader.init(context)
                pdfBoxInitialized.set(true)
            }
        }
    }

    private fun readLimitedText(input: InputStream, maxChars: Int): String {
        val reader = BufferedReader(input.reader(StandardCharsets.UTF_8))
        val out = StringBuilder()
        val buffer = CharArray(1024)
        var remaining = maxChars.coerceAtLeast(256)

        while (remaining > 0) {
            val read = reader.read(buffer, 0, minOf(buffer.size, remaining))
            if (read <= 0) break
            out.append(buffer, 0, read)
            remaining -= read
        }

        return cleanTextForAi(out.toString(), maxChars)
    }

    private fun cleanTextForAi(text: String, maxChars: Int): String {
        val collapsed = text
            .replace("\u0000", " ")
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                    !line.matches(Regex("^p[aá]g(ina)?\\s+\\d+\\s*$", RegexOption.IGNORE_CASE)) &&
                    !line.matches(Regex("^\\d+\\s*/\\s*\\d+$"))
            }
            .joinToString("\n")
            .replace(Regex("[ \t]{2,}"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        return collapsed.take(maxChars.coerceAtLeast(256))
    }

    private fun buildCacheKey(material: Material): String {
        return listOf(
            material.id,
            material.subjectId,
            material.topicId,
            material.title,
            material.uri,
            material.type.name,
            material.createdAt
        ).joinToString("|")
    }

    private fun getCachedSummary(material: Material): String? = synchronized(summaryCache) {
        summaryCache[buildCacheKey(material)]
    }

    private fun cacheSummary(material: Material, summary: String?) {
        if (summary.isNullOrBlank()) return
        synchronized(summaryCache) {
            summaryCache[buildCacheKey(material)] = summary
        }
    }

    private fun invalidateSummaryCache(material: Material) {
        synchronized(summaryCache) {
            summaryCache.keys.removeAll { key ->
                key.startsWith("${material.id}|") ||
                    key.contains("|${material.uri}|")
            }
        }
    }

    private companion object {
        val pdfBoxInitialized = AtomicBoolean(false)
        val summaryCache = object : LinkedHashMap<String, String>(SUMMARY_CACHE_LIMIT, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
                return size > SUMMARY_CACHE_LIMIT
            }
        }
        const val SUMMARY_CACHE_LIMIT = 120
        val TEXT_EXTENSIONS = AiDocumentProcessingPolicy.TEXT_UPLOAD_EXTENSIONS
    }
}
