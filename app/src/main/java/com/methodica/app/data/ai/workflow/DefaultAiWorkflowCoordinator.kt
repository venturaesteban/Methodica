package com.methodica.app.data.ai.workflow

import com.methodica.app.domain.ai.local.LocalModelRuntimeManager
import com.methodica.app.domain.ai.local.RetrievalIndex
import com.methodica.app.domain.ai.local.RetrievalQuery
import com.methodica.app.domain.ai.local.RuntimeAvailability
import com.methodica.app.domain.ai.workflow.AiWorkflowCapability
import com.methodica.app.domain.ai.workflow.AiWorkflowCoordinator
import com.methodica.app.domain.ai.workflow.AnalyzeAssessmentRequest
import com.methodica.app.domain.ai.workflow.ApplyAnalysisEditsRequest
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
            localModelsReady = runtime.availability == RuntimeAvailability.READY,
            runtimeMessage = when {
                runtime.isIndexing -> "Indexando materiales locales…"
                else -> runtime.lastError
            }
        )
    }

    override suspend fun analyzeAssessment(request: AnalyzeAssessmentRequest): Result<StoredAiAnalysis> =
        runCatching {
            val assessment = assessmentRepository.getAssessment(request.assessmentId)
                ?: error("No existe la evaluación ${request.assessmentId}")

            val localEvidence = retrievalIndex.query(
                RetrievalQuery(
                    query = request.rawText,
                    subjectId = assessment.subjectId,
                    assessmentId = request.assessmentId,
                    limit = 4
                )
            ).getOrDefault(emptyList())

            val enrichedPrompt = buildString {
                append(request.rawText)
                if (localEvidence.isNotEmpty()) {
                    append("\n\nContexto recuperado localmente:\n")
                    localEvidence.forEachIndexed { index, hit ->
                        append("- Evidencia ${index + 1} (score=")
                        append("%.3f".format(hit.score))
                        append("): ")
                        append(hit.content.take(360))
                        append('\n')
                    }
                }
            }

            analyzeAssessmentWithAiUseCase(
                assessmentId = request.assessmentId,
                rawText = enrichedPrompt,
                executionMode = request.executionMode,
                sourceLabel = request.sourceLabel
            ).getOrElse { throw it }

            getLatestAiAnalysisForAssessmentUseCase(request.assessmentId)
                ?: error("No se pudo recuperar el análisis generado")
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
}
