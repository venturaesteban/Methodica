package com.methodica.app.domain.usecase.assessment

import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.AssessmentType
import com.methodica.app.domain.repository.AssessmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class UpsertAssessmentUseCaseTest {

    private val useCase = UpsertAssessmentUseCase(FakeAssessmentRepository())

    private fun valid() = Assessment(
        subjectId = 1L,
        type      = AssessmentType.EXAM,
        title     = "Parcial 1",
        date      = 1_700_000_000_000L
    )

    @Test
    fun `falla si el título está vacío`() = runTest {
        val result = useCase(valid().copy(title = ""))
        assertTrue(result.isFailure)
    }

    @Test
    fun `falla si la fecha es cero`() = runTest {
        val result = useCase(valid().copy(date = 0L))
        assertTrue(result.isFailure)
    }

    @Test
    fun `falla si el peso es negativo`() = runTest {
        val result = useCase(valid().copy(weight = -1))
        assertTrue(result.isFailure)
    }

    @Test
    fun `falla si el peso supera 100`() = runTest {
        val result = useCase(valid().copy(weight = 101))
        assertTrue(result.isFailure)
    }

    @Test
    fun `tiene éxito con datos mínimos válidos`() = runTest {
        val result = useCase(valid())
        assertTrue(result.isSuccess)
    }

    @Test
    fun `tiene éxito con peso límite 0`() = runTest {
        val result = useCase(valid().copy(weight = 0))
        assertTrue(result.isSuccess)
    }

    @Test
    fun `tiene éxito con peso límite 100`() = runTest {
        val result = useCase(valid().copy(weight = 100))
        assertTrue(result.isSuccess)
    }
}

private class FakeAssessmentRepository : AssessmentRepository {
    private val assessments = mutableListOf<Assessment>()
    private var nextId = 1L

    override fun observeAllAssessments(): Flow<List<Assessment>> = flowOf(assessments.toList())

    override fun observeAssessments(subjectId: Long): Flow<List<Assessment>> =
        flowOf(assessments.filter { it.subjectId == subjectId })

    override suspend fun getAssessment(id: Long): Assessment? = assessments.find { it.id == id }
    override suspend fun saveAssessment(assessment: Assessment): Long {
        val id = if (assessment.id == 0L) nextId++ else assessment.id
        assessments.removeAll { it.id == id }
        assessments.add(assessment.copy(id = id))
        return id
    }
    override suspend fun deleteAssessment(assessment: Assessment) { assessments.remove(assessment) }
}
