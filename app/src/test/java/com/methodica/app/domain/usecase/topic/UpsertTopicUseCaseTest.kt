package com.methodica.app.domain.usecase.topic

import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class UpsertTopicUseCaseTest {

    private val useCase = UpsertTopicUseCase(FakeTopicRepository())

    @Test
    fun `falla si el nombre está vacío`() = runTest {
        val result = useCase(Topic(subjectId = 1L, name = "", estimatedHours = 2))
        assertTrue(result.isFailure)
    }

    @Test
    fun `falla si la dificultad está fuera de rango (0)`() = runTest {
        val result = useCase(Topic(subjectId = 1L, name = "Tema", difficulty = 0))
        assertTrue(result.isFailure)
    }

    @Test
    fun `falla si la dificultad está fuera de rango (4)`() = runTest {
        val result = useCase(Topic(subjectId = 1L, name = "Tema", difficulty = 4))
        assertTrue(result.isFailure)
    }

    @Test
    fun `falla si estimatedHours es menor que 1`() = runTest {
        val result = useCase(Topic(subjectId = 1L, name = "Tema", estimatedHours = 0))
        assertTrue(result.isFailure)
    }

    @Test
    fun `tiene éxito con datos válidos`() = runTest {
        val result = useCase(Topic(subjectId = 1L, name = "Derivadas", difficulty = 2, estimatedHours = 3))
        assertTrue(result.isSuccess)
    }
}

private class FakeTopicRepository : TopicRepository {
    private val topics = mutableListOf<Topic>()

    override fun observeTopics(subjectId: Long): Flow<List<Topic>> = flowOf(topics.filter { it.subjectId == subjectId })
    override suspend fun getTopic(id: Long): Topic? = topics.find { it.id == id }
    override suspend fun saveTopic(topic: Topic) { topics.add(topic) }
    override suspend fun deleteTopic(topic: Topic) { topics.remove(topic) }
}
