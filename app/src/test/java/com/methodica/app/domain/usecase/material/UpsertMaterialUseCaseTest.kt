package com.methodica.app.domain.usecase.material

import com.methodica.app.domain.ai.local.LocalAiIngestionPipeline
import com.methodica.app.domain.ai.local.LocalIndexingStatus
import com.methodica.app.domain.model.Material
import com.methodica.app.domain.model.MaterialType
import com.methodica.app.domain.repository.MaterialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpsertMaterialUseCaseTest {


    private class FakeIngestionPipeline : LocalAiIngestionPipeline {
        override fun observeStatus(subjectId: Long, materialId: Long?): Flow<LocalIndexingStatus> =
            MutableStateFlow(LocalIndexingStatus(false, null, null, null))

        override suspend fun reindexMaterial(materialId: Long, trigger: String): Result<Unit> = Result.success(Unit)

        override suspend fun deleteMaterialIndex(subjectId: Long, materialId: Long): Result<Unit> = Result.success(Unit)
    }

    private class FakeMaterialRepository : MaterialRepository {
        var saved: Material? = null
        var nextId: Long = 1L

        override fun observeAllMaterials(): Flow<List<Material>> = flowOf(emptyList())

        override suspend fun getMaterial(id: Long): Material? = null

        override suspend fun saveMaterial(material: Material): Long {
            saved = material
            return nextId
        }

        override suspend fun deleteMaterial(material: Material) = Unit

        override suspend fun buildAiResourceSummary(material: Material, maxChars: Int): String? = null

        override suspend fun buildAiIndexableContent(material: Material, maxChars: Int): String? = null
    }

    @Test
    fun `falla si subjectId no es valido`() = runTest {
        val repo = FakeMaterialRepository()
        val useCase = UpsertMaterialUseCase(repo, FakeIngestionPipeline())

        val result = useCase(
            Material(subjectId = 0L, title = "Guía", uri = "https://example.com", type = MaterialType.WEB_LINK)
        )

        assertTrue(result.isFailure)
        assertFalse(repo.saved != null)
    }

    @Test
    fun `falla si titulo esta vacio`() = runTest {
        val repo = FakeMaterialRepository()
        val useCase = UpsertMaterialUseCase(repo, FakeIngestionPipeline())

        val result = useCase(
            Material(subjectId = 10L, title = " ", uri = "https://example.com", type = MaterialType.WEB_LINK)
        )

        assertTrue(result.isFailure)
        assertFalse(repo.saved != null)
    }

    @Test
    fun `falla si uri esta vacia`() = runTest {
        val repo = FakeMaterialRepository()
        val useCase = UpsertMaterialUseCase(repo, FakeIngestionPipeline())

        val result = useCase(
            Material(subjectId = 10L, title = "Resumen", uri = "", type = MaterialType.WEB_LINK)
        )

        assertTrue(result.isFailure)
        assertFalse(repo.saved != null)
    }

    @Test
    fun `guarda material valido`() = runTest {
        val repo = FakeMaterialRepository()
        val useCase = UpsertMaterialUseCase(repo, FakeIngestionPipeline())

        val result = useCase(
            Material(subjectId = 10L, topicId = 2L, title = "Tema 2", uri = "content://doc/1", type = MaterialType.FILE_URI)
        )

        assertTrue(result.isSuccess)
        assertTrue(repo.saved != null)
    }
}
