package com.methodica.app.domain.usecase.subject

import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class UpsertSubjectUseCaseTest {

    private val useCase = UpsertSubjectUseCase(FakeSubjectRepository())

    @Test
    fun `falla si el nombre está vacío`() = runTest {
        val result = useCase(Subject(name = "", colorHex = "#FF5722"))
        assertTrue(result.isFailure)
    }

    @Test
    fun `falla si el nombre solo contiene espacios`() = runTest {
        val result = useCase(Subject(name = "   ", colorHex = "#FF5722"))
        assertTrue(result.isFailure)
    }

    @Test
    fun `falla si el colorHex está vacío`() = runTest {
        val result = useCase(Subject(name = "Matemáticas", colorHex = ""))
        assertTrue(result.isFailure)
    }

    @Test
    fun `tiene éxito con datos válidos`() = runTest {
        val result = useCase(Subject(name = "Matemáticas", colorHex = "#FF5722"))
        assertTrue(result.isSuccess)
    }
}

private class FakeSubjectRepository : SubjectRepository {
    private val subjects = mutableListOf<Subject>()

    override fun observeSubjects(): Flow<List<Subject>> = flowOf(subjects)
    override suspend fun getSubject(id: Long): Subject? = subjects.find { it.id == id }
    override suspend fun saveSubject(subject: Subject) { subjects.add(subject) }
    override suspend fun deleteSubject(subject: Subject) { subjects.remove(subject) }
}
