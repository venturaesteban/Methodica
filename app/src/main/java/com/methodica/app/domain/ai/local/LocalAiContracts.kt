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
    val displayName: String,
    val version: String,
    val localRelativePath: String,
    val requiredDiskBytes: Long,
    val requiredRamMb: Int,
    val expectedSha256: String? = null,
    val redistributionRequiresLicenseConfirmation: Boolean = false
)

data class DeviceCompatibilityReport(
    val isSupported: Boolean,
    val availableDiskBytes: Long,
    val availableRamMb: Int,
    val blockers: List<String>
)

enum class RuntimeAvailability {
    UNINITIALIZED,
    DOWNLOADING,
    INITIALIZING,
    READY,
    ERROR
}

data class LocalModelRuntimeState(
    val availability: RuntimeAvailability,
    val isIndexing: Boolean,
    val installedModels: Set<LocalAiModelType>,
    val lastError: String? = null
)

enum class LocalModelInstallStatus {
    NOT_INSTALLED,
    DOWNLOADING,
    VERIFYING,
    INSTALLING,
    READY,
    ERROR,
    INCOMPATIBLE_DEVICE,
    NO_SPACE
}

data class DownloadableLocalModelDescriptor(
    val id: String,
    val version: String,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val requiredRamMb: Int,
    val requiredDiskBytes: Long,
    val supportedAbis: List<String>,
    val minSdk: Int
)

data class DownloadableLocalModelManifest(
    val manifestVersion: String,
    val models: List<DownloadableLocalModelDescriptor>
)

data class LocalModelInstallState(
    val type: LocalAiModelType,
    val modelId: String,
    val displayName: String,
    val modelVersion: String,
    val status: LocalModelInstallStatus,
    val localRelativePath: String,
    val requiredDiskBytes: Long,
    val requiredRamMb: Int,
    val supportedAbis: List<String>,
    val minSdk: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val lastError: String?,
    val updatedAt: Long,
    val isDownloadConfigured: Boolean,
    val redistributionRequiresLicenseConfirmation: Boolean
) {
    val progressPercent: Int?
        get() = if (totalBytes <= 0L) null else ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)

    val isBusy: Boolean
        get() = status in setOf(
            LocalModelInstallStatus.DOWNLOADING,
            LocalModelInstallStatus.VERIFYING,
            LocalModelInstallStatus.INSTALLING
        )
}

interface LocalModelRuntimeManager {
    fun observeRuntimeState(): Flow<LocalModelRuntimeState>
    fun observeModelInstallStates(): Flow<List<LocalModelInstallState>>
    suspend fun evaluateDeviceCompatibility(spec: LocalAiModelSpec): DeviceCompatibilityReport
    suspend fun refreshDownloadableModels(): Result<Unit>
    suspend fun requestModelDownload(type: LocalAiModelType): Result<Unit>
    suspend fun cancelModelDownload(type: LocalAiModelType): Result<Unit>
    suspend fun deleteInstalledModel(type: LocalAiModelType): Result<Unit>
    suspend fun ensureModelReady(spec: LocalAiModelSpec): Result<Unit>
    suspend fun markModelError(type: LocalAiModelType, message: String)
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
    val tokenEstimate: Int,
    val sourceLabel: String,
    val resourceType: String,
    val section: String?,
    val contentHash: String,
    val updatedAt: Long
)

data class ChunkEmbedding(
    val chunkExternalId: String,
    val dimensions: Int,
    val values: FloatArray
)

data class RetrievalQuery(
    val query: String,
    val subjectId: Long,
    val assessmentId: Long? = null,
    val topicId: Long? = null,
    val materialId: Long? = null,
    val documentId: Long? = null,
    val limit: Int = 5
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
        rawText: String,
        resourceType: String,
        updatedAt: Long = System.currentTimeMillis()
    ): List<TextChunk>
}

interface EmbeddingProvider {
    val modelVersion: String
    suspend fun embed(chunks: List<TextChunk>): Result<List<ChunkEmbedding>>
}

interface RetrievalIndex {
    suspend fun upsert(chunks: List<TextChunk>, embeddings: List<ChunkEmbedding>): Result<Unit>
    suspend fun query(request: RetrievalQuery): Result<List<RetrievalHit>>
    suspend fun markSourceDirty(source: ChunkSourceRef): Result<Unit>
}

data class ReasoningRequest(
    val assessmentId: Long,
    val prompt: String,
    val evidence: List<RetrievalHit>
)

interface ReasoningProvider {
    suspend fun reason(request: ReasoningRequest): Result<ReasoningPlanOutput>
}

data class ActionRequest(
    val assessmentId: Long,
    val actionName: String,
    val payload: Map<String, String>
)

interface ActionProvider {
    suspend fun execute(request: ActionRequest): Result<Unit>
}
