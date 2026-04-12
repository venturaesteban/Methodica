package com.methodica.app.data.ai.workflow

import com.methodica.app.domain.ai.local.ReasoningProvider
import com.methodica.app.domain.ai.local.ReasoningRequest
import com.methodica.app.domain.ai.local.LocalModelRuntimeManager
import com.methodica.app.domain.ai.local.LocalAiModelType
import com.methodica.app.domain.ai.local.RetrievalIndex
import com.methodica.app.domain.ai.local.RetrievalQuery
import com.methodica.app.domain.ai.local.RuntimeAvailability
import com.methodica.app.domain.ai.workflow.AiWorkflowCapability
import com.methodica.app.domain.ai.workflow.AiWorkflowCoordinator
import com.methodica.app.domain.ai.workflow.AnalyzeAssessmentRequest
import com.methodica.app.domain.ai.workflow.ApplyAnalysisEditsRequest
import com.methodica.app.domain.model.AiAnalysis
import com.methodica.app.domain.model.AiDocument
import com.methodica.app.domain.model.AiDocumentSourceType
import com.methodica.app.domain.model.ExamScopeAnalysis
import com.methodica.app.domain.model.TopicComplexityAnalysis
import com.methodica.app.domain.repository.AiAnalysisRepository
import com.methodica.app.domain.repository.AssessmentRepository
import com.methodica.app.domain.repository.StoredAiAnalysis
import com.methodica.app.domain.usecase.ai.AnalyzeAssessmentWithAiUseCase
import com.methodica.app.domain.usecase.ai.ApplyAiComplexityToTopicsUseCase
import com.methodica.app.domain.usecase.ai.GetLatestAiAnalysisForAssessmentUseCase
import com.methodica.app.domain.usecase.ai.ObserveAiProviderSettingsUseCase
import com.methodica.app.domain.usecase.ai.SaveAiAnalysisEditsUseCase
import com.methodica.app.domain.usecase.assessmenttopic.ObserveAssessmentTopicsUseCase
import com.methodica.app.domain.usecase.planning.GenerateAssessmentPlanUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

@Singleton
class DefaultAiWorkflowCoordinator @Inject constructor(
    private val observeAiProviderSettingsUseCase: ObserveAiProviderSettingsUseCase,
    private val localModelRuntimeManager: LocalModelRuntimeManager,
    private val assessmentRepository: AssessmentRepository,
    private val retrievalIndex: RetrievalIndex,
    private val reasoningProvider: ReasoningProvider,
    private val aiAnalysisRepository: AiAnalysisRepository,
    private val analyzeAssessmentWithAiUseCase: AnalyzeAssessmentWithAiUseCase,
    private val getLatestAiAnalysisForAssessmentUseCase: GetLatestAiAnalysisForAssessmentUseCase,
    private val saveAiAnalysisEditsUseCase: SaveAiAnalysisEditsUseCase,
    private val observeAssessmentTopicsUseCase: ObserveAssessmentTopicsUseCase,
    private val applyAiComplexityToTopicsUseCase: ApplyAiComplexityToTopicsUseCase,
    private val generateAssessmentPlanUseCase: GenerateAssessmentPlanUseCase
) : AiWorkflowCoordinator {

    override fun observeCapabilities(): Flow<AiWorkflowCapability> = combine(
        observeAiProviderSettingsUseCase(),
        localModelRuntimeManager.observeRuntimeState()
    ) { settings, runtime ->
        AiWorkflowCapability(
            canUseExternalAi = settings.isEnabledAndConfigured,
            localModelsReady = runtime.availability == RuntimeAvailability.READY &&
                runtime.installedModels.contains(LocalAiModelType.GEMMA_3N_REASONING),
            runtimeMessage = when {
                runtime.isIndexing -> "Indexando materiales locales…"
                runtime.availability == RuntimeAvailability.INITIALIZING -> "Inicializando Gemma 3n local…"
                runtime.availability == RuntimeAvailability.DOWNLOADING -> "Preparando runtime local de Gemma 3n…"
                else -> runtime.lastError
            }
        )
    }

    override suspend fun analyzeAssessment(request: AnalyzeAssessmentRequest): Result<StoredAiAnalysis> =
        runCatching {
            val assessment = assessmentRepository.getAssessment(request.assessmentId)
                ?: error("No existe la evaluación ${request.assessmentId}")

            val linkedTopics = observeAssessmentTopicsUseCase(request.assessmentId).first()
            val retrievalQueries = buildList {
                add(request.rawText.ifBlank { assessment.title })
                add(assessment.title)
                linkedTopics.take(4).forEach { add(it.name) }
            }.distinct()

            val evidence = retrievalQueries.flatMap { queryText ->
                retrievalIndex.query(
                    RetrievalQuery(
                        query = queryText,
                        subjectId = assessment.subjectId,
                        assessmentId = request.assessmentId,
                        limit = 4
                    )
                ).getOrDefault(emptyList())
            }
                .sortedByDescending { it.score }
                .distinctBy { it.chunkExternalId }
                .take(8)

            val runtimeSnapshot = localModelRuntimeManager.observeRuntimeState().first()
            val canRunGemmaReasoning = runtimeSnapshot.availability == RuntimeAvailability.READY &&
                runtimeSnapshot.installedModels.contains(LocalAiModelType.GEMMA_3N_REASONING)

            if (!canRunGemmaReasoning) {
                return@runCatching runHeuristicFallback(
                    request = request,
                    evidence = evidence,
                    reason = runtimeSnapshot.lastError ?: "Gemma local no disponible en este dispositivo"
                )
            }

            val reasoningResult = reasoningProvider.reason(
                ReasoningRequest(
                    assessmentId = request.assessmentId,
                    prompt = request.rawText.ifBlank {
                        "Analiza el alcance evaluable y complejidad de ${assessment.title} con evidencia local."
                    },
                    evidence = evidence
                )
            )

            if (reasoningResult.isSuccess) {
                val output = reasoningResult.getOrThrow()
                aiAnalysisRepository.storeAnalysis(
                    document = AiDocument(
                        subjectId = assessment.subjectId,
                        assessmentId = request.assessmentId,
                        materialId = null,
                        sourceType = AiDocumentSourceType.RAW_TEXT,
                        sourceLabel = "Gemma 3n local (${request.sourceLabel})",
                        extractedText = buildString {
                            append("Prompt: ")
                            append(request.rawText)
                            append("\n\nEvidencia usada:\n")
                            evidence.forEachIndexed { index, hit ->
                                append("${index + 1}. ")
                                append(hit.content.take(320))
                                append('\n')
                            }
                        }
                    ),
                    analysis = AiAnalysis(
                        assessmentId = request.assessmentId,
                        documentId = 0,
                        summary = output.summary,
                        confidence = output.confidence,
                        requiresConfirmation = output.scope.requiresUserConfirmation
                    ),
                    scope = ExamScopeAnalysis(
                        analysisId = 0,
                        estimatedScope = output.scope.estimatedScope,
                        justification = output.scope.justification,
                        confidence = output.scope.confidence,
                        requiresUserConfirmation = output.scope.requiresUserConfirmation
                    ),
                    topicComplexities = output.topicComplexities.map { topic ->
                        TopicComplexityAnalysis(
                            analysisId = 0,
                            topicName = topic.topicName,
                            isIncludedInScope = topic.isIncludedInScope,
                            complexityLevel = topic.complexityLevel,
                            recommendedHours = topic.recommendedHours,
                            priority = topic.priority,
                            requiresPractice = topic.requiresPractice,
                            requiresSpacedReview = topic.requiresSpacedReview,
                            rationale = topic.rationale
                        )
                    }
                )
            } else {
                runHeuristicFallback(
                    request = request,
                    evidence = evidence,
                    reason = reasoningResult.exceptionOrNull()?.message ?: "Error de inferencia local"
                )
            }
        }

    override suspend fun getLatestAnalysis(assessmentId: Long): StoredAiAnalysis? =
        getLatestAiAnalysisForAssessmentUseCase(assessmentId)

    override suspend fun saveAnalysisEdits(request: ApplyAnalysisEditsRequest): Result<Unit> =
        saveAiAnalysisEditsUseCase(
            analysisId = request.analysisId,
            estimatedScope = request.estimatedScope,
            justification = request.justification,
            topicComplexities = request.topicComplexities
        )

    override suspend fun applyEditsAndRegenerate(request: ApplyAnalysisEditsRequest): Result<Unit> = runCatching {
        saveAnalysisEdits(request).getOrElse { throw it }

        val topics = observeAssessmentTopicsUseCase(request.assessmentId).first()
        applyAiComplexityToTopicsUseCase(
            originalTopics = topics,
            editedComplexities = request.topicComplexities
        ).getOrElse { throw it }

        generateAssessmentPlanUseCase(request.assessmentId).getOrElse { throw it }
    }

    private suspend fun runHeuristicFallback(
        request: AnalyzeAssessmentRequest,
        evidence: List<com.methodica.app.domain.ai.local.RetrievalHit>,
        reason: String
    ): StoredAiAnalysis {
        val enrichedPrompt = buildString {
            append(request.rawText)
            append("\n\nFALLBACK_HEURISTICO: runtime local no disponible.\n")
            append("Motivo: ")
            append(reason)
            append('\n')
            evidence.forEachIndexed { index, hit ->
                append("- Evidencia ${index + 1}: ")
                append(hit.content.take(280))
                append('\n')
            }
        }

        analyzeAssessmentWithAiUseCase(
            assessmentId = request.assessmentId,
            rawText = enrichedPrompt,
            executionMode = request.executionMode,
            sourceLabel = "Fallback heurístico (Gemma local no operativa)"
        ).getOrElse { throw it }

        return getLatestAiAnalysisForAssessmentUseCase(request.assessmentId)
            ?: error("No se pudo recuperar el análisis de fallback")
    }
}
