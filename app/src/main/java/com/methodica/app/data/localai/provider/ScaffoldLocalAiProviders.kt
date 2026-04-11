package com.methodica.app.data.localai.provider

import com.methodica.app.domain.ai.local.ActionProvider
import com.methodica.app.domain.ai.local.ActionRequest
import com.methodica.app.domain.ai.local.ChunkEmbedding
import com.methodica.app.domain.ai.local.ChunkingStrategy
import com.methodica.app.domain.ai.local.ChunkSourceRef
import com.methodica.app.domain.ai.local.EmbeddingProvider
import com.methodica.app.domain.ai.local.ReasoningProvider
import com.methodica.app.domain.ai.local.ReasoningRequest
import com.methodica.app.domain.ai.local.RetrievalHit
import com.methodica.app.domain.ai.local.RetrievalIndex
import com.methodica.app.domain.ai.local.TextChunk
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParagraphChunkingStrategy @Inject constructor() : ChunkingStrategy {
    override fun chunkDocument(source: ChunkSourceRef, sourceLabel: String, rawText: String): List<TextChunk> {
        if (rawText.isBlank()) return emptyList()
        return rawText
            .split("\n\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapIndexed { index, paragraph ->
                TextChunk(
                    externalId = "$sourceLabel-$index",
                    source = source,
                    sequence = index,
                    content = paragraph,
                    tokenEstimate = paragraph.length / 4
                )
            }
    }
}

@Singleton
class DeferredEmbeddingProvider @Inject constructor() : EmbeddingProvider {
    override suspend fun embed(chunks: List<TextChunk>): Result<List<ChunkEmbedding>> =
        Result.failure(IllegalStateException("EmbeddingGemma aún no integrado en esta fase"))
}

@Singleton
class RoomBackedRetrievalIndex @Inject constructor() : RetrievalIndex {
    override suspend fun upsert(chunks: List<TextChunk>, embeddings: List<ChunkEmbedding>): Result<Unit> =
        Result.success(Unit)

    override suspend fun query(
        query: String,
        subjectId: Long,
        assessmentId: Long?,
        limit: Int
    ): Result<List<RetrievalHit>> = Result.success(emptyList())

    override suspend fun markSourceDirty(source: ChunkSourceRef): Result<Unit> = Result.success(Unit)
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
