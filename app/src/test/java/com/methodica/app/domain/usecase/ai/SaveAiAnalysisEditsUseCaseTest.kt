package com.methodica.app.domain.usecase.ai

import com.methodica.app.domain.model.AiAnalysis
import com.methodica.app.domain.model.AiDocument
import com.methodica.app.domain.model.AiDocumentSourceType
import com.methodica.app.domain.model.ExamScopeAnalysis
import com.methodica.app.domain.model.TopicComplexityAnalysis
import com.methodica.app.domain.repository.AiAnalysisRepository
import com.methodica.app.domain.repository.StoredAiAnalysis
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveAiAnalysisEditsUseCaseTest {

    private class FakeAiAnalysisRepository : AiAnalysisRepository {
        var called = false

        override suspend fun storeAnalysis(
            document: AiDocument,
            analysis: AiAnalysis,
            scope: ExamScopeAnalysis,
            topicComplexities: List<TopicComplexityAnalysis>
        ): StoredAiAnalysis = StoredAiAnalysis(document, analysis, scope, topicComplexities)

        override suspend fun getLatestAnalysisForAssessment(assessmentId: Long): StoredAiAnalysis? = null

        override suspend fun saveUserEdits(
            analysisId: Long,
            estimatedScope: String,
            justification: String,
            topicComplexities: List<TopicComplexityAnalysis>
        ) {
            called = true
        }
    }

    @Test
    fun `falla si alcance vacio`() = runTest {
        val repo = FakeAiAnalysisRepository()
        val useCase = SaveAiAnalysisEditsUseCase(repo)

        val result = useCase(
            analysisId = 1L,
            estimatedScope = "",
            justification = "ok",
            topicComplexities = emptyList()
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `guarda si datos validos`() = runTest {
        val repo = FakeAiAnalysisRepository()
        val useCase = SaveAiAnalysisEditsUseCase(repo)

        val result = useCase(
            analysisId = 1L,
            estimatedScope = "Temas 1-3",
            justification = "Detectado en guía",
            topicComplexities = listOf(
                TopicComplexityAnalysis(
                    id = 1L,
                    analysisId = 1L,
                    topicName = "Tema 1",
                    isIncludedInScope = true,
                    complexityLevel = 3,
                    recommendedHours = 4,
                    priority = 100,
                    requiresPractice = true,
                    requiresSpacedReview = false,
                    rationale = "test"
                )
            )
        )

        assertTrue(result.isSuccess)
        assertTrue(repo.called)
    }
}
