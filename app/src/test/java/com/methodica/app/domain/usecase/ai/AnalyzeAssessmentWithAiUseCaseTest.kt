package com.methodica.app.domain.usecase.ai

import com.methodica.app.data.ai.HeuristicComplexityEstimator
import com.methodica.app.data.ai.HeuristicDocumentParser
import com.methodica.app.data.ai.HeuristicExamScopeInferenceService
import com.methodica.app.data.ai.HeuristicStudyPlanningAdvisor
import com.methodica.app.data.ai.HeuristicSyllabusAnalyzer
import com.methodica.app.domain.ai.LlmProvider
import com.methodica.app.domain.model.AiAnalysis
import com.methodica.app.domain.model.AiExecutionMode
import com.methodica.app.domain.model.AiDocument
import com.methodica.app.domain.model.AiProviderSettings
import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.AssessmentType
import com.methodica.app.domain.model.ExamScopeAnalysis
import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.model.TopicComplexityAnalysis
import com.methodica.app.domain.repository.AiAnalysisRepository
import com.methodica.app.domain.repository.AiProviderSettingsRepository
import com.methodica.app.domain.repository.AssessmentRepository
import com.methodica.app.domain.repository.AssessmentTopicRepository
import com.methodica.app.domain.repository.StoredAiAnalysis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeAssessmentWithAiUseCaseTest {

    private val validAcademicText = """
        Tema 1. Límites y continuidad
        Tema 2. Derivadas y aplicaciones
        Tema 3. Integrales definidas
        Tema 4. Ecuaciones diferenciales básicas
        Tema 5. Optimización
        Evaluación: contenido evaluable de los temas 1 al 5.
        Criterios de evaluación: resolución de problemas, justificación de pasos y ejercicios tipo examen.
        Prueba final escrita con preguntas teóricas y prácticas.
        Objetivos: modelar problemas, analizar funciones y aplicar técnicas de cálculo.
        Bibliografía, prácticas y ejercicios semanales para repaso.
        """.trimIndent().repeat(6)

    private class FakeAssessmentRepository(
        private val assessment: Assessment?
    ) : AssessmentRepository {
        override fun observeAllAssessments(): Flow<List<Assessment>> = flowOf(emptyList())
        override fun observeAssessments(subjectId: Long): Flow<List<Assessment>> = flowOf(emptyList())
        override suspend fun getAssessment(id: Long): Assessment? = assessment
        override suspend fun saveAssessment(assessment: Assessment): Long = 1L
        override suspend fun deleteAssessment(assessment: Assessment) = Unit
    }

    private class FakeAssessmentTopicRepository(
        private val topics: List<Topic>
    ) : AssessmentTopicRepository {
        override fun observeTopicsForAssessment(assessmentId: Long): Flow<List<Topic>> = flowOf(topics)
        override suspend fun getTopicsForAssessment(assessmentId: Long): List<Topic> = topics
        override fun observeTopicIdsForAssessment(assessmentId: Long): Flow<List<Long>> = flowOf(topics.map { it.id })
        override suspend fun replaceTopicsForAssessment(assessmentId: Long, topicIds: List<Long>) = Unit
    }

    private class FakeAiAnalysisRepository : AiAnalysisRepository {
        override suspend fun storeAnalysis(
            document: AiDocument,
            analysis: AiAnalysis,
            scope: ExamScopeAnalysis,
            topicComplexities: List<TopicComplexityAnalysis>
        ): StoredAiAnalysis {
            return StoredAiAnalysis(
                document = document.copy(id = 1L),
                analysis = analysis.copy(id = 2L, documentId = 1L),
                scope = scope.copy(id = 3L, analysisId = 2L),
                topicComplexities = topicComplexities.mapIndexed { index, item ->
                    item.copy(id = index + 10L, analysisId = 2L)
                }
            )
        }

        override suspend fun getLatestAnalysisForAssessment(assessmentId: Long): StoredAiAnalysis? = null

        override suspend fun saveUserEdits(
            analysisId: Long,
            estimatedScope: String,
            justification: String,
            topicComplexities: List<TopicComplexityAnalysis>
        ) = Unit
    }

    private class FakeAiProviderSettingsRepository : AiProviderSettingsRepository {
        override fun observeSettings(): Flow<AiProviderSettings> =
            flowOf(AiProviderSettings())

        override suspend fun saveSettings(settings: AiProviderSettings) = Unit
    }

    private class FakeConfiguredAiProviderSettingsRepository(
        private val settings: AiProviderSettings
    ) : AiProviderSettingsRepository {
        override fun observeSettings(): Flow<AiProviderSettings> =
            flowOf(settings)

        override suspend fun saveSettings(settings: AiProviderSettings) = Unit
    }

    private class FakeLlmProvider : LlmProvider {
        override suspend fun generate(
            baseUrl: String,
            model: String,
            apiKey: String,
            prompt: String
        ): String? = null
    }

    private class TrackingLlmProvider : LlmProvider {
        var called = false

        override suspend fun generate(
            baseUrl: String,
            model: String,
            apiKey: String,
            prompt: String
        ): String? {
            called = true
            return """
                Tema 1. Límites y continuidad
                Tema 2. Derivadas y aplicaciones
                Tema 3. Integrales definidas
                Tema 4. Ecuaciones diferenciales básicas
                Tema 5. Optimización
                Evaluación: contenido evaluable de los temas 1 al 5.
                Criterios de evaluación: resolución de problemas, justificación de pasos y ejercicios tipo examen.
            """.trimIndent().repeat(6)
        }
    }

    @Test
    fun `genera insight y persiste analisis`() = runTest {
        val assessment = Assessment(
            id = 1L,
            subjectId = 10L,
            type = AssessmentType.EXAM,
            title = "Parcial 1",
            date = System.currentTimeMillis() + 86400000L
        )
        val topics = listOf(
            Topic(id = 1L, subjectId = 10L, name = "Tema 1", difficulty = 2, estimatedHours = 3, order = 0)
        )

        val useCase = AnalyzeAssessmentWithAiUseCase(
            assessmentRepository = FakeAssessmentRepository(assessment),
            assessmentTopicRepository = FakeAssessmentTopicRepository(topics),
            aiAnalysisRepository = FakeAiAnalysisRepository(),
            aiProviderSettingsRepository = FakeAiProviderSettingsRepository(),
            llmProvider = FakeLlmProvider(),
            documentParser = HeuristicDocumentParser(),
            syllabusAnalyzer = HeuristicSyllabusAnalyzer(),
            examScopeInferenceService = HeuristicExamScopeInferenceService(),
            complexityEstimator = HeuristicComplexityEstimator(),
            studyPlanningAdvisor = HeuristicStudyPlanningAdvisor()
        )

        val result = useCase(
            assessmentId = 1L,
            rawText = validAcademicText
        )

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.complexityAnalysis.topics.isNotEmpty())
    }

    @Test
    fun `falla si texto esta vacio`() = runTest {
        val assessment = Assessment(
            id = 1L,
            subjectId = 10L,
            type = AssessmentType.EXAM,
            title = "Parcial 1",
            date = System.currentTimeMillis() + 86400000L
        )

        val useCase = AnalyzeAssessmentWithAiUseCase(
            assessmentRepository = FakeAssessmentRepository(assessment),
            assessmentTopicRepository = FakeAssessmentTopicRepository(emptyList()),
            aiAnalysisRepository = FakeAiAnalysisRepository(),
            aiProviderSettingsRepository = FakeAiProviderSettingsRepository(),
            llmProvider = FakeLlmProvider(),
            documentParser = HeuristicDocumentParser(),
            syllabusAnalyzer = HeuristicSyllabusAnalyzer(),
            examScopeInferenceService = HeuristicExamScopeInferenceService(),
            complexityEstimator = HeuristicComplexityEstimator(),
            studyPlanningAdvisor = HeuristicStudyPlanningAdvisor()
        )

        val result = useCase(assessmentId = 1L, rawText = "")
        assertTrue(result.isFailure)
    }

    @Test
    fun `usa proveedor externo cuando el modo es external y esta configurado`() = runTest {
        val assessment = Assessment(
            id = 1L,
            subjectId = 10L,
            type = AssessmentType.EXAM,
            title = "Parcial 1",
            date = System.currentTimeMillis() + 86400000L
        )
        val trackingProvider = TrackingLlmProvider()

        val useCase = AnalyzeAssessmentWithAiUseCase(
            assessmentRepository = FakeAssessmentRepository(assessment),
            assessmentTopicRepository = FakeAssessmentTopicRepository(emptyList()),
            aiAnalysisRepository = FakeAiAnalysisRepository(),
            aiProviderSettingsRepository = FakeConfiguredAiProviderSettingsRepository(
                AiProviderSettings(
                    externalEnabled = true,
                    providerName = "OpenAI",
                    baseUrl = "https://api.openai.com/v1/chat/completions",
                    model = "gpt-4o-mini",
                    apiKey = "test-key"
                )
            ),
            llmProvider = trackingProvider,
            documentParser = HeuristicDocumentParser(),
            syllabusAnalyzer = HeuristicSyllabusAnalyzer(),
            examScopeInferenceService = HeuristicExamScopeInferenceService(),
            complexityEstimator = HeuristicComplexityEstimator(),
            studyPlanningAdvisor = HeuristicStudyPlanningAdvisor()
        )

        val result = useCase(
            assessmentId = 1L,
            rawText = validAcademicText,
            executionMode = AiExecutionMode.EXTERNAL
        )

        assertTrue(result.isSuccess)
        assertTrue(trackingProvider.called)
    }
}
