package com.methodica.app.data.localai

import com.methodica.app.data.local.dao.AiChunkEmbeddingDao
import com.methodica.app.data.local.dao.AiDocumentChunkDao
import com.methodica.app.data.local.dao.AiIndexingRunDao
import com.methodica.app.data.local.dao.EmbeddingChunkRow
import com.methodica.app.data.local.entity.AiChunkEmbeddingEntity
import com.methodica.app.data.local.entity.AiDocumentChunkEntity
import com.methodica.app.data.local.entity.AiIndexingRunEntity
import com.methodica.app.data.localai.pipeline.DefaultLocalAiIngestionPipeline
import com.methodica.app.data.localai.provider.ParagraphChunkingStrategy
import com.methodica.app.data.localai.provider.RoomBackedRetrievalIndex
import com.methodica.app.domain.ai.local.ChunkEmbedding
import com.methodica.app.domain.ai.local.ChunkSourceRef
import com.methodica.app.domain.ai.local.EmbeddingProvider
import com.methodica.app.domain.ai.local.RetrievalHit
import com.methodica.app.domain.ai.local.RetrievalIndex
import com.methodica.app.domain.ai.local.RetrievalQuery
import com.methodica.app.domain.ai.local.TextChunk
import com.methodica.app.domain.model.Material
import com.methodica.app.domain.model.MaterialType
import com.methodica.app.domain.repository.MaterialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAiPipelineTest {

    @Test
    fun `chunking genera hash y metadatos`() {
        val strategy = ParagraphChunkingStrategy()
        val chunks = strategy.chunkDocument(
            source = ChunkSourceRef(1, null, 5, 8, null),
            sourceLabel = "apuntes-tema5",
            rawText = "Introduccion\n\nEste es un bloque largo de prueba sobre algebra lineal.\n\nMatrices\n\nLas matrices permiten resolver sistemas.",
            resourceType = "FILE_URI",
            updatedAt = 123L
        )

        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.all { it.contentHash.isNotBlank() })
        assertTrue(chunks.all { it.resourceType == "FILE_URI" })
    }

    @Test
    fun `retrieval aplica filtros academicos`() = runTest {
        val chunkDao = FakeChunkDao()
        val embeddingDao = FakeEmbeddingDao(chunkDao)
        val provider = FakeSemanticEmbeddingProvider()
        val index = RoomBackedRetrievalIndex(chunkDao, embeddingDao, provider)

        val a = sampleChunk("a", ChunkSourceRef(10, 20, 30, 40, null), "derivadas e integrales")
        val b = sampleChunk("b", ChunkSourceRef(10, 21, 31, 41, null), "historia contemporanea")
        val embeddings = provider.embed(listOf(a, b)).getOrThrow()
        index.upsert(listOf(a, b), embeddings).getOrThrow()

        val hits = index.query(RetrievalQuery(query = "integrales", subjectId = 10, assessmentId = 20, materialId = 40, limit = 5)).getOrThrow()

        assertEquals(1, hits.size)
        assertEquals("a", hits.first().chunkExternalId)
    }

    @Test
    fun `indexacion incremental evita reembed si hash no cambia`() = runTest {
        val materialRepo = FakeMaterialRepository()
        val chunkDao = FakeChunkDao()
        val runDao = FakeRunDao()
        val embeddingDao = FakeEmbeddingDao(chunkDao)
        val provider = CountingEmbeddingProvider()
        val pipeline = DefaultLocalAiIngestionPipeline(
            materialRepository = materialRepo,
            chunkingStrategy = ParagraphChunkingStrategy(),
            embeddingProvider = provider,
            retrievalIndex = RoomBackedRetrievalIndex(chunkDao, embeddingDao, provider),
            chunkDao = chunkDao,
            embeddingDao = embeddingDao,
            indexingRunDao = runDao
        )

        pipeline.reindexMaterial(1, "TEST").getOrThrow()
        val first = provider.calls
        pipeline.reindexMaterial(1, "TEST").getOrThrow()

        assertEquals(first, provider.calls)
    }

    @Test
    fun `reindexa cuando version de embedding cambia`() = runTest {
        val materialRepo = FakeMaterialRepository()
        val chunkDao = FakeChunkDao()
        val runDao = FakeRunDao()
        val embeddingDao = FakeEmbeddingDao(chunkDao)
        val firstProvider = CountingEmbeddingProvider(modelVersion = "semantic-v1")
        val secondProvider = CountingEmbeddingProvider(modelVersion = "semantic-v2")

        DefaultLocalAiIngestionPipeline(
            materialRepository = materialRepo,
            chunkingStrategy = ParagraphChunkingStrategy(),
            embeddingProvider = firstProvider,
            retrievalIndex = RoomBackedRetrievalIndex(chunkDao, embeddingDao, firstProvider),
            chunkDao = chunkDao,
            embeddingDao = embeddingDao,
            indexingRunDao = runDao
        ).reindexMaterial(1, "TEST").getOrThrow()

        DefaultLocalAiIngestionPipeline(
            materialRepository = materialRepo,
            chunkingStrategy = ParagraphChunkingStrategy(),
            embeddingProvider = secondProvider,
            retrievalIndex = RoomBackedRetrievalIndex(chunkDao, embeddingDao, secondProvider),
            chunkDao = chunkDao,
            embeddingDao = embeddingDao,
            indexingRunDao = runDao
        ).reindexMaterial(1, "TEST").getOrThrow()

        assertTrue(secondProvider.calls > 0)
    }

    private fun sampleChunk(id: String, source: ChunkSourceRef, content: String) = TextChunk(
        externalId = id,
        source = source,
        sequence = 0,
        content = content,
        tokenEstimate = 3,
        sourceLabel = "src",
        resourceType = "FILE",
        section = null,
        contentHash = content,
        updatedAt = 1L
    )

    private class CountingEmbeddingProvider(
        override val modelVersion: String = "test-semantic-v2"
    ) : EmbeddingProvider {
        var calls = 0
        private val delegate = FakeSemanticEmbeddingProvider(modelVersion)
        override suspend fun embed(chunks: List<TextChunk>): Result<List<ChunkEmbedding>> {
            calls += chunks.size
            return delegate.embed(chunks)
        }
    }

    private class FakeMaterialRepository : MaterialRepository {
        private val material = Material(id = 1, subjectId = 10, topicId = 2, title = "Tema", uri = "content://x", type = MaterialType.FILE_URI)
        override fun observeAllMaterials(): Flow<List<Material>> = flowOf(listOf(material))
        override suspend fun getMaterial(id: Long): Material? = material
        override suspend fun saveMaterial(material: Material): Long = material.id
        override suspend fun deleteMaterial(material: Material) = Unit
        override suspend fun buildAiResourceSummary(material: Material, maxChars: Int): String? = "summary"
        override suspend fun buildAiIndexableContent(material: Material, maxChars: Int): String? = "Tema 1\n\nIntegrales y derivadas"
    }

    private class FakeRunDao : AiIndexingRunDao {
        private val last = MutableStateFlow<AiIndexingRunEntity?>(null)
        private val running = MutableStateFlow(0)
        private var nextId = 1L
        override suspend fun insert(run: AiIndexingRunEntity): Long {
            val id = nextId++
            last.value = run.copy(id = id)
            running.value = 1
            return id
        }

        override suspend fun finish(id: Long, status: String, completedAt: Long, errorMessage: String?) {
            last.value = last.value?.copy(status = status, completedAt = completedAt, errorMessage = errorMessage)
            running.value = 0
        }

        override fun observeLastRun(): Flow<AiIndexingRunEntity?> = last
        override fun observeRunningCount(): Flow<Int> = running
    }

    private class FakeChunkDao : AiDocumentChunkDao {
        private val items = mutableListOf<AiDocumentChunkEntity>()
        private var nextId = 1L

        override suspend fun upsert(chunks: List<AiDocumentChunkEntity>) {
            chunks.forEach { chunk ->
                val index = items.indexOfFirst { it.externalId == chunk.externalId }
                if (index >= 0) items[index] = chunk.copy(id = items[index].id) else items += chunk.copy(id = nextId++)
            }
        }

        override suspend fun getByExternalIds(externalIds: List<String>): List<AiDocumentChunkEntity> =
            items.filter { it.externalId in externalIds }

        override suspend fun getBySource(subjectId: Long, assessmentId: Long?, topicId: Long?, materialId: Long?, documentId: Long?): List<AiDocumentChunkEntity> =
            items.filter {
                it.subjectId == subjectId &&
                    (assessmentId == null || it.assessmentId == assessmentId) &&
                    (topicId == null || it.topicId == topicId) &&
                    (materialId == null || it.materialId == materialId) &&
                    (documentId == null || it.documentId == documentId)
            }

        override suspend fun deleteByIds(ids: List<Long>) { items.removeAll { it.id in ids } }
        override suspend fun deleteByContext(subjectId: Long, assessmentId: Long?) = Unit
        override fun observeIndexedChunkCount(subjectId: Long, materialId: Long?): Flow<Int> =
            MutableStateFlow(items.count { it.subjectId == subjectId && (materialId == null || it.materialId == materialId) })

        fun byId(id: Long): AiDocumentChunkEntity? = items.firstOrNull { it.id == id }
    }

    private class FakeEmbeddingDao(private val chunkDao: FakeChunkDao) : AiChunkEmbeddingDao {
        private val rows = mutableListOf<AiChunkEmbeddingEntity>()
        override suspend fun upsert(embeddings: List<AiChunkEmbeddingEntity>) {
            embeddings.forEach { emb ->
                rows.removeAll { it.chunkId == emb.chunkId }
                rows += emb
            }
        }

        override suspend fun deleteByChunkIds(chunkIds: List<Long>) { rows.removeAll { it.chunkId in chunkIds } }

        override suspend fun getIndexedRows(subjectId: Long, assessmentId: Long?, topicId: Long?, materialId: Long?, documentId: Long?, modelVersion: String): List<EmbeddingChunkRow> =
            rows.mapNotNull { emb ->
                val chunk = chunkDao.byId(emb.chunkId) ?: return@mapNotNull null
                if (emb.modelVersion != modelVersion) return@mapNotNull null
                if (chunk.subjectId != subjectId) return@mapNotNull null
                if (assessmentId != null && chunk.assessmentId != assessmentId) return@mapNotNull null
                if (topicId != null && chunk.topicId != topicId) return@mapNotNull null
                if (materialId != null && chunk.materialId != materialId) return@mapNotNull null
                if (documentId != null && chunk.documentId != documentId) return@mapNotNull null
                EmbeddingChunkRow(chunk.externalId, chunk.subjectId, chunk.assessmentId, chunk.topicId, chunk.materialId, chunk.documentId, chunk.content, emb.vector, emb.dimensions)
            }

        override suspend fun getModelVersionsForChunkIds(chunkIds: List<Long>): List<String> =
            rows.filter { it.chunkId in chunkIds }.map { it.modelVersion }.distinct()
    }

    private class FakeSemanticEmbeddingProvider(
        override val modelVersion: String = "test-semantic-v2"
    ) : EmbeddingProvider {
        override suspend fun embed(chunks: List<TextChunk>): Result<List<ChunkEmbedding>> = runCatching {
            chunks.map { chunk ->
                val tokens = chunk.content.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
                val vector = FloatArray(3)
                tokens.forEach {
                    when {
                        it.contains("integral") || it.contains("derivad") -> vector[0] += 1f
                        it.contains("historia") || it.contains("contempor") -> vector[1] += 1f
                        else -> vector[2] += 0.3f
                    }
                }
                val norm = kotlin.math.sqrt(vector.fold(0f) { acc, value -> acc + (value * value) }.toDouble()).toFloat().takeIf { it > 0f } ?: 1f
                for (i in vector.indices) vector[i] = vector[i] / norm
                ChunkEmbedding(chunk.externalId, vector.size, vector)
            }
        }
    }
}
