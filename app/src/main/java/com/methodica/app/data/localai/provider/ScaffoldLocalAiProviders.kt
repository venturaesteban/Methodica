package com.methodica.app.data.localai.provider

import com.methodica.app.data.local.dao.AiChunkEmbeddingDao
import com.methodica.app.data.local.dao.AiDocumentChunkDao
import com.methodica.app.data.local.entity.AiChunkEmbeddingEntity
import com.methodica.app.data.local.entity.AiDocumentChunkEntity
import com.methodica.app.domain.ai.local.ActionProvider
import com.methodica.app.domain.ai.local.ActionRequest
import com.methodica.app.domain.ai.local.ChunkEmbedding
import com.methodica.app.domain.ai.local.ChunkingStrategy
import com.methodica.app.domain.ai.local.EmbeddingProvider
import com.methodica.app.domain.ai.local.ReasoningProvider
import com.methodica.app.domain.ai.local.ReasoningRequest
import com.methodica.app.domain.ai.local.RetrievalHit
import com.methodica.app.domain.ai.local.RetrievalIndex
import com.methodica.app.domain.ai.local.RetrievalQuery
import com.methodica.app.domain.ai.local.TextChunk
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class ParagraphChunkingStrategy @Inject constructor() : ChunkingStrategy {
    override fun chunkDocument(
        source: com.methodica.app.domain.ai.local.ChunkSourceRef,
        sourceLabel: String,
        rawText: String,
        resourceType: String,
        updatedAt: Long
    ): List<TextChunk> {
        if (rawText.isBlank()) return emptyList()
        val normalized = rawText
            .replace("\r", "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        val paragraphs = normalized
            .split("\n\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val chunks = mutableListOf<TextChunk>()
        val window = StringBuilder()
        var section: String? = null
        var sequence = 0

        fun flush() {
            val content = window.toString().trim()
            if (content.isBlank()) return
            val hash = sha256(content)
            chunks += TextChunk(
                externalId = "$sourceLabel-$sequence-${hash.take(10)}",
                source = source,
                sequence = sequence,
                content = content,
                tokenEstimate = (content.length / 4).coerceAtLeast(1),
                sourceLabel = sourceLabel,
                resourceType = resourceType,
                section = section,
                contentHash = hash,
                updatedAt = updatedAt
            )
            sequence += 1
            window.clear()
        }

        paragraphs.forEach { paragraph ->
            val maybeHeading = paragraph.take(120)
            if (maybeHeading.length < 90 && maybeHeading.matches(Regex("^[0-9A-Za-zÁÉÍÓÚáéíóúÜüÑñ .:_-]{3,}$"))) {
                section = maybeHeading
            }

            if (window.isNotEmpty() && window.length + paragraph.length > MAX_CHARS_PER_CHUNK) {
                flush()
            }
            if (window.isNotEmpty()) window.append("\n\n")
            window.append(paragraph)

            while (window.length > HARD_MAX_CHARS) {
                val splitAt = window.lastIndexOf(" ", HARD_MAX_CHARS)
                    .takeIf { it > HARD_MIN_CHARS } ?: HARD_MAX_CHARS
                val head = window.substring(0, splitAt).trim()
                val tail = window.substring(splitAt).trim()
                window.clear()
                window.append(head)
                flush()
                if (tail.isNotBlank()) window.append(tail)
            }
        }
        flush()
        return chunks
    }

    private fun sha256(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val MAX_CHARS_PER_CHUNK = 900
        const val HARD_MAX_CHARS = 1200
        const val HARD_MIN_CHARS = 400
    }
}

@Singleton
class OnDeviceEmbeddingProvider @Inject constructor() : EmbeddingProvider {
    override suspend fun embed(chunks: List<TextChunk>): Result<List<ChunkEmbedding>> = runCatching {
        chunks.map { chunk ->
            val vector = FloatArray(DIMENSIONS)
            val tokens = TOKEN_REGEX.findAll(chunk.content.lowercase()).map { it.value }.toList()
            val sizeNorm = if (tokens.isEmpty()) 1f else 1f / tokens.size
            tokens.forEach { token ->
                val hash = token.hashCode()
                val idx = (hash and Int.MAX_VALUE) % DIMENSIONS
                val sign = if (hash % 2 == 0) 1f else -1f
                vector[idx] += sign * sizeNorm
            }
            normalize(vector)
            ChunkEmbedding(chunkExternalId = chunk.externalId, dimensions = DIMENSIONS, values = vector)
        }
    }

    private fun normalize(values: FloatArray) {
        var normSq = 0f
        values.forEach { normSq += it * it }
        val norm = sqrt(normSq).takeIf { it > 0f } ?: return
        for (i in values.indices) {
            values[i] /= norm
        }
    }

    private companion object {
        const val DIMENSIONS = 256
        val TOKEN_REGEX = Regex("[\\p{L}\\p{N}_-]{2,}")
    }
}

@Singleton
class RoomBackedRetrievalIndex @Inject constructor(
    private val chunkDao: AiDocumentChunkDao,
    private val embeddingDao: AiChunkEmbeddingDao,
    private val embeddingProvider: OnDeviceEmbeddingProvider
) : RetrievalIndex {
    override suspend fun upsert(chunks: List<TextChunk>, embeddings: List<ChunkEmbedding>): Result<Unit> = runCatching {
        if (chunks.isEmpty()) return@runCatching
        val embeddingByExternalId = embeddings.associateBy { it.chunkExternalId }
        val now = System.currentTimeMillis()

        chunkDao.upsert(
            chunks.map {
                AiDocumentChunkEntity(
                    externalId = it.externalId,
                    subjectId = it.source.subjectId,
                    assessmentId = it.source.assessmentId,
                    topicId = it.source.topicId,
                    materialId = it.source.materialId,
                    documentId = it.source.documentId,
                    sequence = it.sequence,
                    sourceLabel = "${it.sourceLabel}|${it.resourceType}|${it.section.orEmpty()}",
                    content = it.content,
                    tokenEstimate = it.tokenEstimate,
                    contentHash = it.contentHash,
                    updatedAt = it.updatedAt
                )
            }
        )

        val stored = chunkDao.getByExternalIds(chunks.map { it.externalId })
        val embeddingsToPersist = stored.mapNotNull { chunk ->
            val e = embeddingByExternalId[chunk.externalId] ?: return@mapNotNull null
            AiChunkEmbeddingEntity(
                chunkId = chunk.id,
                modelVersion = MODEL_VERSION,
                vector = e.values.joinToString(",") { value -> value.toString() },
                dimensions = e.dimensions,
                updatedAt = now
            )
        }
        if (embeddingsToPersist.isNotEmpty()) {
            embeddingDao.upsert(embeddingsToPersist)
        }
    }

    override suspend fun query(request: RetrievalQuery): Result<List<RetrievalHit>> = runCatching {
        val pseudoChunk = TextChunk(
            externalId = "query",
            source = com.methodica.app.domain.ai.local.ChunkSourceRef(request.subjectId, request.assessmentId, request.topicId, request.materialId, request.documentId),
            sequence = 0,
            content = request.query,
            tokenEstimate = request.query.length / 4,
            sourceLabel = "query",
            resourceType = "query",
            section = null,
            contentHash = request.query,
            updatedAt = System.currentTimeMillis()
        )
        val queryEmbedding = embeddingProvider.embed(listOf(pseudoChunk)).getOrThrow().first().values
        val candidates = embeddingDao.getIndexedRows(
            subjectId = request.subjectId,
            assessmentId = request.assessmentId,
            topicId = request.topicId,
            materialId = request.materialId,
            documentId = request.documentId
        )

        candidates.mapNotNull { row ->
            val vector = row.vector.split(',').mapNotNull { it.toFloatOrNull() }
            if (vector.size != row.dimensions) return@mapNotNull null
            val score = cosine(queryEmbedding, vector)
            RetrievalHit(
                chunkExternalId = row.externalId,
                source = com.methodica.app.domain.ai.local.ChunkSourceRef(row.subjectId, row.assessmentId, row.topicId, row.materialId, row.documentId),
                content = row.content,
                score = score
            )
        }.sortedByDescending { it.score }
            .take(request.limit.coerceIn(1, 20))
    }

    override suspend fun markSourceDirty(source: com.methodica.app.domain.ai.local.ChunkSourceRef): Result<Unit> = runCatching {
        val stale = chunkDao.getBySource(
            subjectId = source.subjectId,
            assessmentId = source.assessmentId,
            topicId = source.topicId,
            materialId = source.materialId,
            documentId = source.documentId
        )
        if (stale.isNotEmpty()) {
            chunkDao.deleteByIds(stale.map { it.id })
        }
    }

    private fun cosine(query: FloatArray, candidate: List<Float>): Float {
        val size = minOf(query.size, candidate.size)
        var dot = 0f
        var qNorm = 0f
        var cNorm = 0f
        for (i in 0 until size) {
            val q = query[i]
            val c = candidate[i]
            dot += q * c
            qNorm += q * q
            cNorm += c * c
        }
        val denom = sqrt(qNorm) * sqrt(cNorm)
        return if (denom <= 0f) 0f else dot / denom
    }

    private companion object {
        const val MODEL_VERSION = "hashing-embedding-v1"
    }
}

@Singleton
class DeferredReasoningProvider @Inject constructor() : ReasoningProvider {
    override suspend fun reason(request: ReasoningRequest): Result<String> =
        Result.failure(IllegalStateException("Gemma 3n local aún no integrada en esta fase"))
}

@Singleton
class DeferredActionProvider @Inject constructor() : ActionProvider {
    override suspend fun execute(request: ActionRequest): Result<Unit> =
        Result.failure(IllegalStateException("FunctionGemma aún no integrada en esta fase"))
}
