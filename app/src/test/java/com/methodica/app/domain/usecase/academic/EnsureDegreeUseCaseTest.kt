package com.methodica.app.domain.usecase.academic

import com.methodica.app.domain.model.AcademicYear
import com.methodica.app.domain.model.Degree
import com.methodica.app.domain.model.DegreeStatus
import com.methodica.app.domain.repository.AcademicCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnsureDegreeUseCaseTest {

    @Test
    fun `falla si la titulacion esta vacia`() = runTest {
        val useCase = EnsureDegreeUseCase(FakeAcademicCatalogRepository())

        val result = useCase(" ")

        assertTrue(result.isFailure)
    }

    @Test
    fun `crea titulacion valida`() = runTest {
        val repo = FakeAcademicCatalogRepository()
        val useCase = EnsureDegreeUseCase(repo)

        val result = useCase("Ingenieria Informatica")

        assertTrue(result.isSuccess)
        assertEquals("Ingenieria Informatica", repo.lastEnsuredDegreeName)
    }
}

private class FakeAcademicCatalogRepository : AcademicCatalogRepository {
    var lastEnsuredDegreeName: String? = null

    override fun observeDegrees(): Flow<List<Degree>> = flowOf(emptyList())

    override fun observeDegreesByStatus(status: DegreeStatus): Flow<List<Degree>> = flowOf(emptyList())

    override fun observeAcademicYears(degreeId: Long): Flow<List<AcademicYear>> = flowOf(emptyList())

    override suspend fun getDegree(id: Long): Degree? = null

    override suspend fun getAcademicYear(id: Long): AcademicYear? = null

    override suspend fun upsertDegree(degree: Degree): Long = 1L

    override suspend fun updateDegreeStatus(id: Long, status: DegreeStatus) = Unit

    override suspend fun deleteDegree(id: Long) = Unit

    override suspend fun deleteAcademicYear(id: Long) = Unit

    override suspend fun ensureDegree(name: String): Long {
        lastEnsuredDegreeName = name.trim()
        return 1L
    }

    override suspend fun ensureAcademicYear(degreeId: Long, yearNumber: Int): Long = 1L
}

