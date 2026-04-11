package com.methodica.app.domain.ai.local

import kotlinx.coroutines.flow.Flow

enum class LocalAiModelType {
    EMBEDDING_GEMMA,
    GEMMA_3N_REASONING,
    FUNCTION_GEMMA
}

data class LocalAiModelSpec(
    val id: String,
    val type: LocalAiModelType,
    val version: String,
    val assetPath: String,
    val requiredDiskBytes: Long,
    val requiredRamMb: Int
)

data class DeviceCompatibilityReport(
    val isSupported: Boolean,
    val availableDiskBytes: Long,
    val availableRamMb: Int,
    val blockers: List<String>
)

enum class RuntimeAvailability {
    UNINITIALIZED,
    READY,
    ERROR
}

data class LocalModelRuntimeState(
    val availability: RuntimeAvailability,
    val isIndexing: Boolean,
    val installedModels: Set<LocalAiModelType>,
    val lastError: String? = null
)

interface LocalModelRuntimeManager {
    fun observeRuntimeState(): Flow<LocalModelRuntimeState>
    suspend fun evaluateDeviceCompatibility(spec: LocalAiModelSpec): DeviceCompatibilityReport
    suspend fun ensureModelReady(spec: LocalAiModelSpec): Result<Unit>
    suspend fun releaseModels()
}

data class ChunkSourceRef(
    val subjectId: Long,
    val assessmentId: Long?,
    val topicId: Long?,
    val materialId: Long?,
    val documentId: Long?
)

data class TextChunk(
    val externalId: String,
    val source: ChunkSourceRef,
    val sequence: Int,
    val content: String,
    val tokenEstimate: Int
)

data class ChunkEmbedding(
    val chunkExternalId: String,
    val dimensions: Int,
    val values: FloatArray
)

data class RetrievalHit(
    val chunkExternalId: String,
    val source: ChunkSourceRef,
    val content: String,
    val score: Float
)

interface ChunkingStrategy {
    fun chunkDocument(
        source: ChunkSourceRef,
        sourceLabel: String,
        rawText: String
    ): List<TextChunk>
}

interface EmbeddingProvider {
    suspend fun embed(chunks: List<TextChunk>): Result<List<ChunkEmbedding>>
}

interface RetrievalIndex {
    suspend fun upsert(chunks: List<TextChunk>, embeddings: List<ChunkEmbedding>): Result<Unit>
    suspend fun query(
        query: String,
        subjectId: Long,
        assessmentId: Long?,
        limit: Int
    ): Result<List<RetrievalHit>>
    suspend fun markSourceDirty(source: ChunkSourceRef): Result<Unit>
}

data class ReasoningRequest(
    val assessmentId: Long,
    val prompt: String,
    val evidence: List<RetrievalHit>
)

interface ReasoningProvider {
    suspend fun reason(request: ReasoningRequest): Result<String>
}

data class ActionRequest(
    val assessmentId: Long,
    val actionName: String,
    val payload: Map<String, String>
)

interface ActionProvider {
    suspend fun execute(request: ActionRequest): Result<Unit>
}
