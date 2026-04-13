package com.methodica.app.data.ai.workflow

import com.methodica.app.data.ai.HeuristicComplexityEstimator
import com.methodica.app.data.ai.HeuristicDocumentParser
import com.methodica.app.data.ai.HeuristicExamScopeInferenceService
import com.methodica.app.data.ai.HeuristicStudyPlanningAdvisor
import com.methodica.app.data.ai.HeuristicSyllabusAnalyzer
import com.methodica.app.domain.ai.LlmProvider
import com.methodica.app.domain.ai.local.ChunkSourceRef
import com.methodica.app.domain.ai.local.LocalAiModelSpec
import com.methodica.app.domain.ai.local.LocalAiModelType
import com.methodica.app.domain.ai.local.LocalModelInstallState
import com.methodica.app.domain.ai.local.LocalModelRuntimeManager
import com.methodica.app.domain.ai.local.LocalModelRuntimeState
import com.methodica.app.domain.ai.local.ReasoningPlanOutput
import com.methodica.app.domain.ai.local.ReasoningProvider
import com.methodica.app.domain.ai.local.ReasoningRequest
import com.methodica.app.domain.ai.local.ReasoningScope
import com.methodica.app.domain.ai.local.ReasoningSequencingStep
import com.methodica.app.domain.ai.local.ReasoningTopicComplexity
import com.methodica.app.domain.ai.local.RetrievalHit
import com.methodica.app.domain.ai.local.RetrievalIndex
import com.methodica.app.domain.ai.local.RetrievalQuery
import com.methodica.app.domain.ai.local.RuntimeAvailability
import com.methodica.app.domain.ai.workflow.AnalyzeAssessmentRequest
import com.methodica.app.domain.model.AiAnalysis
import com.methodica.app.domain.model.AiExecutionMode
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
import com.methodica.app.domain.usecase.ai.AnalyzeAssessmentWithAiUseCase
import com.methodica.app.domain.usecase.ai.ApplyAiComplexityToTopicsUseCase
import com.methodica.app.domain.usecase.ai.GetLatestAiAnalysisForAssessmentUseCase
import com.methodica.app.domain.usecase.ai.ObserveAiProviderSettingsUseCase
import com.methodica.app.domain.usecase.ai.SaveAiAnalysisEditsUseCase
import com.methodica.app.domain.usecase.assessmenttopic.ObserveAssessmentTopicsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAiWorkflowCoordinatorTest {

    @Test
    fun `usa retrieval local y reasoning principal`() = runTest {
        val aiRepo = InMemoryAiRepo()
        val retrieval = TrackingRetrievalIndex()
        val coordinator = coordinator(aiRepo, retrieval, SuccessReasoningProvider())

        val result = coordinator.analyzeAssessment(
            AnalyzeAssessmentRequest(1, "analiza parcial", "manual", AiExecutionMode.HEURISTIC)
        )

        assertTrue(result.isSuccess)
        assertTrue(retrieval.queries.isNotEmpty())
        assertEquals("Scope local", result.getOrThrow().scope.estimatedScope)
    }

    @Test
    fun `fallback heuristico cuando falla runtime local`() = runTest {
        val aiRepo = InMemoryAiRepo()
        val coordinator = coordinator(aiRepo, TrackingRetrievalIndex(), FailingReasoningProvider())
        val payload = "Tema 1\nTema 2\nTema 3\nTema 4\nTema 5\nCriterios\nObjetivos\nExamen\n".repeat(10)

        val result = coordinator.analyzeAssessment(
            AnalyzeAssessmentRequest(1, payload, "manual", AiExecutionMode.HEURISTIC)
        )

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().analysis.summary.contains("Alcance estimado"))
    }

    @Test
    fun `capabilities no marca reasoning listo sin modelo gemma instalado`() = runTest {
        val aiRepo = InMemoryAiRepo()
        val coordinator = coordinator(
            aiRepo = aiRepo,
            retrievalIndex = TrackingRetrievalIndex(),
            reasoningProvider = SuccessReasoningProvider(),
            runtimeState = MutableStateFlow(
                LocalModelRuntimeState(
                    availability = RuntimeAvailability.READY,
                    isIndexing = false,
                    installedModels = setOf(LocalAiModelType.EMBEDDING_GEMMA)
                )
            )
        )

        val capability = coordinator.observeCapabilities().first()

        assertFalse(capability.localModelsReady)
    }

    private fun coordinator(
        aiRepo: InMemoryAiRepo,
        retrievalIndex: RetrievalIndex,
        reasoningProvider: ReasoningProvider,
        runtimeState: MutableStateFlow<LocalModelRuntimeState> = MutableStateFlow(
            LocalModelRuntimeState(
                availability = RuntimeAvailability.READY,
                isIndexing = false,
                installedModels = setOf(LocalAiModelType.GEMMA_3N_REASONING)
            )
        )
    ): DefaultAiWorkflowCoordinator {
        val assessmentRepository = object : AssessmentRepository {
            override fun observeAllAssessments(): Flow<List<Assessment>> = flowOf(emptyList())
            override fun observeAssessments(subjectId: Long): Flow<List<Assessment>> = flowOf(emptyList())
            override suspend fun getAssessment(id: Long): Assessment? = Assessment(1, 7, AssessmentType.EXAM, "Parcial", System.currentTimeMillis() + 60000)
            override suspend fun saveAssessment(assessment: Assessment): Long = 1
            override suspend fun deleteAssessment(assessment: Assessment) = Unit
        }
        val assessmentTopicRepository = object : AssessmentTopicRepository {
            override fun observeTopicsForAssessment(assessmentId: Long): Flow<List<Topic>> = flowOf(listOf(Topic(1, 7, "Derivadas")))
            override suspend fun getTopicsForAssessment(assessmentId: Long): List<Topic> = listOf(Topic(1, 7, "Derivadas"))
            override fun observeTopicIdsForAssessment(assessmentId: Long): Flow<List<Long>> = flowOf(listOf(1L))
            override suspend fun replaceTopicsForAssessment(assessmentId: Long, topicIds: List<Long>) = Unit
        }
        val fallbackUseCase = AnalyzeAssessmentWithAiUseCase(
            assessmentRepository,
            assessmentTopicRepository,
            aiRepo,
            object : AiProviderSettingsRepository {
                override fun observeSettings(): Flow<AiProviderSettings> = flowOf(AiProviderSettings())
                override suspend fun saveSettings(settings: AiProviderSettings) = Unit
            },
            object : LlmProvider {
                override suspend fun generate(baseUrl: String, model: String, apiKey: String, prompt: String): String? = null
            },
            HeuristicDocumentParser(),
            HeuristicSyllabusAnalyzer(),
            HeuristicExamScopeInferenceService(),
            HeuristicComplexityEstimator(),
            HeuristicStudyPlanningAdvisor()
        )

        return DefaultAiWorkflowCoordinator(
            observeAiProviderSettingsUseCase = ObserveAiProviderSettingsUseCase(object : AiProviderSettingsRepository {
                override fun observeSettings(): Flow<AiProviderSettings> = flowOf(AiProviderSettings())
                override suspend fun saveSettings(settings: AiProviderSettings) = Unit
            }),
            localModelRuntimeManager = object : LocalModelRuntimeManager {
                override fun observeRuntimeState() = runtimeState
                override fun observeModelInstallStates(): Flow<List<LocalModelInstallState>> = flowOf(emptyList())
                override suspend fun evaluateDeviceCompatibility(spec: LocalAiModelSpec) = throw UnsupportedOperationException()
                override suspend fun refreshDownloadableModels() = Result.success(Unit)
                override suspend fun prepareAutomaticModels() = Result.success(Unit)
                override suspend fun requestModelDownload(type: LocalAiModelType) = Result.success(Unit)
                override suspend fun cancelModelDownload(type: LocalAiModelType) = Result.success(Unit)
                override suspend fun deleteInstalledModel(type: LocalAiModelType) = Result.success(Unit)
                override suspend fun ensureModelReady(spec: LocalAiModelSpec) = Result.success(Unit)
                override suspend fun markModelError(type: LocalAiModelType, message: String) = Unit
                override suspend fun releaseModels() = Unit
            },
            assessmentRepository = assessmentRepository,
            retrievalIndex = retrievalIndex,
            reasoningProvider = reasoningProvider,
            aiAnalysisRepository = aiRepo,
            analyzeAssessmentWithAiUseCase = fallbackUseCase,
            getLatestAiAnalysisForAssessmentUseCase = GetLatestAiAnalysisForAssessmentUseCase(aiRepo),
            saveAiAnalysisEditsUseCase = SaveAiAnalysisEditsUseCase(aiRepo),
            observeAssessmentTopicsUseCase = ObserveAssessmentTopicsUseCase(assessmentTopicRepository),
            applyAiComplexityToTopicsUseCase = ApplyAiComplexityToTopicsUseCase(object : com.methodica.app.domain.repository.TopicRepository {
                override fun observeTopics(subjectId: Long): Flow<List<Topic>> = flowOf(emptyList())
                override suspend fun getTopic(id: Long): Topic? = null
                override suspend fun saveTopic(topic: Topic) = Unit
                override suspend fun deleteTopic(topic: Topic) = Unit
            }),
            generateAssessmentPlanUseCase = com.methodica.app.domain.usecase.planning.GenerateAssessmentPlanUseCase(
                assessmentRepository,
                assessmentTopicRepository,
                object : com.methodica.app.domain.repository.StudySessionRepository {
                    override fun observeByDate(date: Long) = flowOf(emptyList<com.methodica.app.domain.model.StudySession>())
                    override fun observeByRange(startDate: Long, endDate: Long) = flowOf(emptyList<com.methodica.app.domain.model.StudySession>())
                    override fun observeByAssessment(assessmentId: Long) = flowOf(emptyList<com.methodica.app.domain.model.StudySession>())
                    override suspend fun getByRange(startDate: Long, endDate: Long) = emptyList<com.methodica.app.domain.model.StudySession>()
                    override suspend fun insertAll(sessions: List<com.methodica.app.domain.model.StudySession>) = Unit
                    override suspend fun updateSession(session: com.methodica.app.domain.model.StudySession) = Unit
                    override suspend fun deletePlannedAutoGeneratedByAssessment(assessmentId: Long) = Unit
                },
                object : com.methodica.app.domain.repository.PlanningSettingsRepository {
                    override fun observeSettings() = flowOf(com.methodica.app.domain.model.PlanningSettings())
                    override suspend fun saveSettings(settings: com.methodica.app.domain.model.PlanningSettings) = Unit
                },
                com.methodica.app.domain.planning.StudyPlanGenerator()
            )
        )
    }

    private class TrackingRetrievalIndex : RetrievalIndex {
        val queries = mutableListOf<RetrievalQuery>()
        override suspend fun upsert(chunks: List<com.methodica.app.domain.ai.local.TextChunk>, embeddings: List<com.methodica.app.domain.ai.local.ChunkEmbedding>) = Result.success(Unit)
        override suspend fun query(request: RetrievalQuery): Result<List<RetrievalHit>> {
            queries += request
            return Result.success(listOf(RetrievalHit("chunk-1", ChunkSourceRef(7, 1, null, null, null), "Derivadas e integrales", 0.9f)))
        }
        override suspend fun markSourceDirty(source: ChunkSourceRef) = Result.success(Unit)
    }

    private class SuccessReasoningProvider : ReasoningProvider {
        override suspend fun reason(request: ReasoningRequest): Result<ReasoningPlanOutput> = Result.success(
            ReasoningPlanOutput(
                scope = ReasoningScope("Scope local", "Basado en evidencia", 0.8f, false),
                topicComplexities = listOf(ReasoningTopicComplexity("Derivadas", 4, 6, 5, true, true, true, "denso")),
                sequencing = listOf(ReasoningSequencingStep(1, "Derivadas", "base")),
                risks = listOf("Tiempo corto"),
                summary = "Resumen local",
                confidence = 0.8f
            )
        )
    }

    private class FailingReasoningProvider : ReasoningProvider {
        override suspend fun reason(request: ReasoningRequest): Result<ReasoningPlanOutput> = Result.failure(IllegalStateException("fallo runtime"))
    }

    private class InMemoryAiRepo : AiAnalysisRepository {
        private var latest: StoredAiAnalysis? = null

        override suspend fun storeAnalysis(
            document: com.methodica.app.domain.model.AiDocument,
            analysis: AiAnalysis,
            scope: ExamScopeAnalysis,
            topicComplexities: List<TopicComplexityAnalysis>
        ): StoredAiAnalysis {
            val stored = StoredAiAnalysis(
                document = document.copy(id = 1),
                analysis = analysis.copy(id = 2, documentId = 1),
                scope = scope.copy(id = 3, analysisId = 2),
                topicComplexities = topicComplexities
            )
            latest = stored
            return stored
        }

        override suspend fun getLatestAnalysisForAssessment(assessmentId: Long): StoredAiAnalysis? = latest

        override suspend fun saveUserEdits(
            analysisId: Long,
            estimatedScope: String,
            justification: String,
            topicComplexities: List<TopicComplexityAnalysis>
        ) = Unit
    }
}
