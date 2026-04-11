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

class EnsureAcademicYearUseCaseTest {

    @Test
    fun `falla si no hay titulacion seleccionada`() = runTest {
        val useCase = EnsureAcademicYearUseCase(FakeAcademicCatalogRepositoryForYear())

        val result = useCase(degreeId = 0L, yearNumber = 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `falla si el curso es invalido`() = runTest {
        val useCase = EnsureAcademicYearUseCase(FakeAcademicCatalogRepositoryForYear())

        val result = useCase(degreeId = 1L, yearNumber = 0)

        assertTrue(result.isFailure)
    }

    @Test
    fun `crea curso academico valido`() = runTest {
        val repo = FakeAcademicCatalogRepositoryForYear()
        val useCase = EnsureAcademicYearUseCase(repo)

        val result = useCase(degreeId = 5L, yearNumber = 3)

        assertTrue(result.isSuccess)
        assertEquals(5L, repo.lastDegreeId)
        assertEquals(3, repo.lastYearNumber)
    }
}

private class FakeAcademicCatalogRepositoryForYear : AcademicCatalogRepository {
    var lastDegreeId: Long? = null
    var lastYearNumber: Int? = null

    override fun observeDegrees(): Flow<List<Degree>> = flowOf(emptyList())

    override fun observeDegreesByStatus(status: DegreeStatus): Flow<List<Degree>> = flowOf(emptyList())

    override fun observeAcademicYears(degreeId: Long): Flow<List<AcademicYear>> = flowOf(emptyList())

    override suspend fun getDegree(id: Long): Degree? = null

    override suspend fun getAcademicYear(id: Long): AcademicYear? = null

    override suspend fun upsertDegree(degree: Degree): Long = 1L

    override suspend fun updateDegreeStatus(id: Long, status: DegreeStatus) = Unit

    override suspend fun deleteDegree(id: Long) = Unit

    override suspend fun deleteAcademicYear(id: Long) = Unit

    override suspend fun ensureDegree(name: String): Long = 1L

    override suspend fun ensureAcademicYear(degreeId: Long, yearNumber: Int): Long {
        lastDegreeId = degreeId
        lastYearNumber = yearNumber
        return 10L
    }
}

