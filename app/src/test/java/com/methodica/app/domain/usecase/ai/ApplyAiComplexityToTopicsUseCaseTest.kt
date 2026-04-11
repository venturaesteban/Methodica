package com.methodica.app.domain.usecase.ai

import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.model.TopicComplexityAnalysis
import com.methodica.app.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplyAiComplexityToTopicsUseCaseTest {

    private class FakeTopicRepository : TopicRepository {
        val saved = mutableListOf<Topic>()

        override fun observeTopics(subjectId: Long): Flow<List<Topic>> = flowOf(emptyList())
        override suspend fun getTopic(id: Long): Topic? = null
        override suspend fun saveTopic(topic: Topic) {
            saved.add(topic)
        }
        override suspend fun deleteTopic(topic: Topic) = Unit
    }

    @Test
    fun `aplica dificultad y horas solo a temas incluidos`() = runTest {
        val repo = FakeTopicRepository()
        val useCase = ApplyAiComplexityToTopicsUseCase(repo)

        val topics = listOf(
            Topic(id = 1L, subjectId = 10L, name = "Tema A", difficulty = 1, estimatedHours = 2, order = 0),
            Topic(id = 2L, subjectId = 10L, name = "Tema B", difficulty = 1, estimatedHours = 2, order = 1)
        )

        val edits = listOf(
            TopicComplexityAnalysis(
                id = 10L,
                analysisId = 1L,
                topicName = "Tema A",
                isIncludedInScope = true,
                complexityLevel = 5,
                recommendedHours = 6,
                priority = 100,
                requiresPractice = true,
                requiresSpacedReview = true,
                rationale = ""
            ),
            TopicComplexityAnalysis(
                id = 11L,
                analysisId = 1L,
                topicName = "Tema B",
                isIncludedInScope = false,
                complexityLevel = 4,
                recommendedHours = 5,
                priority = 90,
                requiresPractice = true,
                requiresSpacedReview = true,
                rationale = ""
            )
        )

        val result = useCase(topics, edits)

        assertEquals(true, result.isSuccess)
        assertEquals(1, repo.saved.size)
        assertEquals("Tema A", repo.saved.first().name)
        assertEquals(3, repo.saved.first().difficulty)
        assertEquals(6, repo.saved.first().estimatedHours)
    }

    @Test
    fun `aplica cambios aunque el nombre tenga acentos o variaciones leves`() = runTest {
        val repo = FakeTopicRepository()
        val useCase = ApplyAiComplexityToTopicsUseCase(repo)

        val topics = listOf(
            Topic(
                id = 1L,
                subjectId = 10L,
                name = "Derivacion numerica",
                difficulty = 1,
                estimatedHours = 3,
                order = 0
            )
        )

        val edits = listOf(
            TopicComplexityAnalysis(
                id = 10L,
                analysisId = 1L,
                topicName = "Derivación numérica",
                isIncludedInScope = true,
                complexityLevel = 4,
                recommendedHours = 5,
                priority = 90,
                requiresPractice = true,
                requiresSpacedReview = true,
                rationale = ""
            )
        )

        val result = useCase(topics, edits)

        assertEquals(true, result.isSuccess)
        assertEquals(1, repo.saved.size)
        assertEquals("Derivacion numerica", repo.saved.first().name)
        assertEquals(3, repo.saved.first().difficulty)
        assertEquals(5, repo.saved.first().estimatedHours)
    }
}
