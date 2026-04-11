package com.methodica.app.data.localai.pipeline

import com.methodica.app.data.local.dao.AiChunkEmbeddingDao
import com.methodica.app.data.local.dao.AiDocumentChunkDao
import com.methodica.app.data.local.dao.AiIndexingRunDao
import com.methodica.app.data.local.entity.AiIndexingRunEntity
import com.methodica.app.domain.ai.local.ChunkSourceRef
import com.methodica.app.domain.ai.local.ChunkingStrategy
import com.methodica.app.domain.ai.local.EmbeddingProvider
import com.methodica.app.domain.ai.local.LocalAiIngestionPipeline
import com.methodica.app.domain.ai.local.LocalIndexingStatus
import com.methodica.app.domain.ai.local.RetrievalIndex
import com.methodica.app.domain.repository.MaterialRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class DefaultLocalAiIngestionPipeline @Inject constructor(
    private val materialRepository: MaterialRepository,
    private val chunkingStrategy: ChunkingStrategy,
    private val embeddingProvider: EmbeddingProvider,
    private val retrievalIndex: RetrievalIndex,
    private val chunkDao: AiDocumentChunkDao,
    private val embeddingDao: AiChunkEmbeddingDao,
    private val indexingRunDao: AiIndexingRunDao
) : LocalAiIngestionPipeline {

    override fun observeStatus(subjectId: Long, materialId: Long?): Flow<LocalIndexingStatus> = combine(
        indexingRunDao.observeRunningCount(),
        indexingRunDao.observeLastRun(),
        chunkDao.observeIndexedChunkCount(subjectId = subjectId, materialId = materialId)
    ) { runningCount, lastRun, chunkCount ->
        LocalIndexingStatus(
            isIndexing = runningCount > 0,
            lastRunStatus = lastRun?.status,
            lastError = lastRun?.errorMessage,
            lastUpdatedAt = lastRun?.completedAt,
            indexedChunks = chunkCount
        )
    }

    override suspend fun reindexMaterial(materialId: Long, trigger: String): Result<Unit> {
        val material = materialRepository.getMaterial(materialId)
            ?: return Result.failure(IllegalArgumentException("Material no encontrado"))
        val runId = startRun(subjectId = material.subjectId, materialId = material.id, trigger = trigger)

        return runCatching {
            val source = ChunkSourceRef(
                subjectId = material.subjectId,
                assessmentId = null,
                topicId = material.topicId,
                materialId = material.id,
                documentId = null
            )

            val indexableContent = materialRepository.buildAiIndexableContent(material)
                ?: materialRepository.buildAiResourceSummary(material)
                ?: ""

            if (indexableContent.isBlank()) {
                retrievalIndex.markSourceDirty(source).getOrThrow()
                finishRun(runId, status = STATUS_SUCCESS, error = null)
                return@runCatching
            }

            val freshChunks = chunkingStrategy.chunkDocument(
                source = source,
                sourceLabel = "material-${material.id}-${material.title.take(24)}",
                rawText = indexableContent,
                resourceType = material.type.name,
                updatedAt = System.currentTimeMillis()
            )

            val existing = chunkDao.getBySource(
                subjectId = source.subjectId,
                assessmentId = source.assessmentId,
                topicId = source.topicId,
                materialId = source.materialId,
                documentId = source.documentId
            )
            val existingByExternal = existing.associateBy { it.externalId }
            val incompatibleVersion = if (existing.isEmpty()) false else {
                embeddingDao.getModelVersionsForChunkIds(existing.map { it.id })
                    .any { it != embeddingProvider.modelVersion }
            }

            val changedOrNew = freshChunks.filter { chunk ->
                val previous = existingByExternal[chunk.externalId]
                incompatibleVersion || previous == null || previous.contentHash != chunk.contentHash
            }
            val removedIds = existing
                .filter { old -> freshChunks.none { it.externalId == old.externalId } }
                .map { it.id }

            var partialError: String? = null
            if (changedOrNew.isNotEmpty()) {
                val embeddings = embeddingProvider.embed(changedOrNew).getOrElse {
                    partialError = it.message ?: "No se pudo generar embeddings"
                    emptyList()
                }
                if (embeddings.isNotEmpty()) {
                    retrievalIndex.upsert(changedOrNew, embeddings).getOrThrow()
                }
            }
            if (removedIds.isNotEmpty()) {
                chunkDao.deleteByIds(removedIds)
            }

            finishRun(
                runId,
                status = if (partialError == null) STATUS_SUCCESS else STATUS_PARTIAL,
                error = partialError
            )
        }.onFailure { error ->
            finishRun(runId, STATUS_FAILED, error.message)
        }
    }

    override suspend fun deleteMaterialIndex(subjectId: Long, materialId: Long): Result<Unit> {
        val source = ChunkSourceRef(
            subjectId = subjectId,
            assessmentId = null,
            topicId = null,
            materialId = materialId,
            documentId = null
        )
        return retrievalIndex.markSourceDirty(source)
    }

    private suspend fun startRun(subjectId: Long, materialId: Long, trigger: String): Long {
        return indexingRunDao.insert(
            AiIndexingRunEntity(
                subjectId = subjectId,
                assessmentId = null,
                materialId = materialId,
                status = STATUS_RUNNING,
                trigger = trigger,
                startedAt = System.currentTimeMillis(),
                completedAt = null,
                errorMessage = null
            )
        )
    }

    private suspend fun finishRun(id: Long, status: String, error: String?) {
        indexingRunDao.finish(id, status, System.currentTimeMillis(), error)
    }

    private companion object {
        const val STATUS_RUNNING = "RUNNING"
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_PARTIAL = "PARTIAL"
        const val STATUS_FAILED = "FAILED"
    }
}
