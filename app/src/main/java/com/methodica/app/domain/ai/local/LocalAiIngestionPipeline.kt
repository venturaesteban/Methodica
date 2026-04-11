package com.methodica.app.domain.ai.local

import kotlinx.coroutines.flow.Flow

data class LocalIndexingStatus(
    val isIndexing: Boolean,
    val lastRunStatus: String?,
    val lastError: String?,
    val lastUpdatedAt: Long?,
    val indexedChunks: Int = 0
)

interface LocalAiIngestionPipeline {
    fun observeStatus(subjectId: Long, materialId: Long? = null): Flow<LocalIndexingStatus>
    suspend fun reindexMaterial(materialId: Long, trigger: String = "MANUAL"): Result<Unit>
    suspend fun deleteMaterialIndex(subjectId: Long, materialId: Long): Result<Unit>
}
