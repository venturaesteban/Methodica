package com.methodica.app.domain.usecase.subject

import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class UpsertSubjectUseCaseTest {

    private val useCase = UpsertSubjectUseCase(FakeSubjectRepository())

    @Test
    fun `falla si el nombre está vacío`() = runTest {
        val result = useCase(Subject(name = "", colorHex = "#FF5722", academicYearId = 1L))
        assertTrue(result.isFailure)
    }

    @Test
    fun `falla si el nombre solo contiene espacios`() = runTest {
        val result = useCase(Subject(name = "   ", colorHex = "#FF5722", academicYearId = 1L))
        assertTrue(result.isFailure)
    }

    @Test
    fun `falla si el colorHex está vacío`() = runTest {
        val result = useCase(Subject(name = "Matemáticas", colorHex = "", academicYearId = 1L))
        assertTrue(result.isFailure)
    }

    @Test
    fun `falla si el curso academico no es valido`() = runTest {
        val result = useCase(Subject(name = "Matemáticas", colorHex = "#FF5722", academicYearId = 0L))
        assertTrue(result.isFailure)
    }

    @Test
    fun `tiene éxito con datos válidos`() = runTest {
        val result = useCase(Subject(name = "Matemáticas", colorHex = "#FF5722", academicYearId = 1L))
        assertTrue(result.isSuccess)
    }
}

private class FakeSubjectRepository : SubjectRepository {
    private val subjects = mutableListOf<Subject>()

    override fun observeSubjects(): Flow<List<Subject>> = flowOf(subjects)
    override fun observeSubjectsByAcademicYearId(academicYearId: Long): Flow<List<Subject>> = 
        flowOf(subjects.filter { it.academicYearId == academicYearId })
    override suspend fun getSubject(id: Long): Subject? = subjects.find { it.id == id }
    override suspend fun saveSubject(subject: Subject) { subjects.add(subject) }
    override suspend fun deleteSubject(subject: Subject) { subjects.remove(subject) }
}
