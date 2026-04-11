package com.methodica.app.domain.usecase.ai

import com.methodica.app.domain.ai.ComplexityEstimator
import com.methodica.app.domain.ai.DocumentParser
import com.methodica.app.domain.ai.ExamScopeInferenceService
import com.methodica.app.domain.ai.LlmProvider
import com.methodica.app.domain.ai.StudyPlanningAdvisor
import com.methodica.app.domain.ai.SyllabusAnalyzer
import com.methodica.app.domain.ai.model.AiPlanningInsight
import com.methodica.app.domain.model.AiAnalysis
import com.methodica.app.domain.model.AiDocument
import com.methodica.app.domain.model.AiDocumentSourceType
import com.methodica.app.domain.model.AiExecutionMode
import com.methodica.app.domain.model.ExamScopeAnalysis
import com.methodica.app.domain.model.TopicComplexityAnalysis
import com.methodica.app.domain.repository.AiAnalysisRepository
import com.methodica.app.domain.repository.AiProviderSettingsRepository
import com.methodica.app.domain.repository.AssessmentRepository
import com.methodica.app.domain.repository.AssessmentTopicRepository
import kotlinx.coroutines.flow.first

class AnalyzeAssessmentWithAiUseCase(
    private val assessmentRepository: AssessmentRepository,
    private val assessmentTopicRepository: AssessmentTopicRepository,
    private val aiAnalysisRepository: AiAnalysisRepository,
    private val aiProviderSettingsRepository: AiProviderSettingsRepository,
    private val llmProvider: LlmProvider,
    private val documentParser: DocumentParser,
    private val syllabusAnalyzer: SyllabusAnalyzer,
    private val examScopeInferenceService: ExamScopeInferenceService,
    private val complexityEstimator: ComplexityEstimator,
    private val studyPlanningAdvisor: StudyPlanningAdvisor
) {

    private companion object {
        const val MIN_HEADINGS = 3
        const val MIN_TOPICS = 5
        const val MIN_EXAM_SIGNALS = 2
    }

    suspend operator fun invoke(
        assessmentId: Long,
        rawText: String,
        executionMode: AiExecutionMode = AiExecutionMode.HEURISTIC,
        sourceLabel: String = "Entrada manual"
    ): Result<AiPlanningInsight> = runCatching {
        if (rawText.isBlank()) error("Debes pegar contenido textual para analizar.")

        val assessment = assessmentRepository.getAssessment(assessmentId)
            ?: error("Evaluación no encontrada")

        val topics = assessmentTopicRepository.getTopicsForAssessment(assessmentId)

        val processedText = resolveTextByMode(
            executionMode = executionMode,
            assessmentTitle = assessment.title,
            rawText = rawText
        )

        val parsed = documentParser.parse(sourceLabel = sourceLabel, rawText = processedText)
        if (parsed.blockedReason != null) {
            error(parsed.blockedReason)
        }

        val analyzed = syllabusAnalyzer.analyze(parsed)
        val hasMinimumStructure =
            analyzed.sections.size >= MIN_HEADINGS ||
                analyzed.detectedTopics.size >= MIN_TOPICS ||
                analyzed.examSignals.size >= MIN_EXAM_SIGNALS

        if (!hasMinimumStructure) {
            error(
                "Documento poco estructurado para análisis heurístico fiable. " +
                    "Incluye secciones, listado de temas y criterios de evaluación."
            )
        }

        val scope = examScopeInferenceService.infer(assessment, analyzed)
        val complexity = complexityEstimator.estimate(analyzed, topics)
        val recommendation = studyPlanningAdvisor.recommend(scope, complexity)

        val summary = buildString {
            append("Alcance estimado con confianza ${"%.0f".format(scope.confidence * 100)}%.")
            if (recommendation.extraReviewTopics.isNotEmpty()) {
                append(" Requieren repaso extra: ${recommendation.extraReviewTopics.joinToString()}." )
            }
            if (analyzed.qualityWarnings.isNotEmpty()) {
                append(" Advertencias: ${analyzed.qualityWarnings.joinToString(" | ")}." )
            }
            if (executionMode == AiExecutionMode.EXTERNAL) {
                append(" Modo IA externa: puede consumir créditos de tu suscripción.")
            }
        }

        val stored = aiAnalysisRepository.storeAnalysis(
            document = AiDocument(
                subjectId = assessment.subjectId,
                assessmentId = assessmentId,
                materialId = null,
                sourceType = AiDocumentSourceType.RAW_TEXT,
                sourceLabel = sourceLabel,
                extractedText = analyzed.plainText
            ),
            analysis = AiAnalysis(
                assessmentId = assessmentId,
                documentId = 0L,
                summary = summary,
                confidence = ((scope.confidence + complexity.confidence + recommendation.confidence) / 3f).coerceIn(0f, 1f),
                requiresConfirmation = scope.requiresConfirmation
            ),
            scope = ExamScopeAnalysis(
                analysisId = 0L,
                estimatedScope = scope.estimatedScope,
                justification = scope.justification,
                confidence = scope.confidence,
                requiresUserConfirmation = scope.requiresConfirmation
            ),
            topicComplexities = complexity.topics.map {
                TopicComplexityAnalysis(
                    analysisId = 0L,
                    topicName = it.topicName,
                    isIncludedInScope = scope.includedTopicNames.isEmpty() || it.topicName in scope.includedTopicNames,
                    complexityLevel = it.complexityLevel,
                    recommendedHours = it.estimatedHours,
                    priority = it.priority,
                    requiresPractice = it.requiresPractice,
                    requiresSpacedReview = it.requiresSpacedReview,
                    rationale = it.rationale
                )
            }
        )

        AiPlanningInsight(
            scopeInference = scope,
            complexityAnalysis = complexity,
            recommendation = recommendation,
            summary = stored.analysis.summary,
            confidence = stored.analysis.confidence,
            requiresConfirmation = stored.analysis.requiresConfirmation
        )
    }

    private suspend fun resolveTextByMode(
        executionMode: AiExecutionMode,
        assessmentTitle: String,
        rawText: String
    ): String {
        if (executionMode == AiExecutionMode.HEURISTIC) return rawText

        val settings = aiProviderSettingsRepository.observeSettings().first()
        if (!settings.isEnabledAndConfigured) {
            error("Activa y configura tu proveedor de IA en Ajustes antes de usar IA externa")
        }

        val prompt = buildExternalPrompt(assessmentTitle = assessmentTitle, rawText = rawText)
        val generated = runCatching {
            llmProvider.generate(
                baseUrl = settings.baseUrl,
                model = settings.model,
                apiKey = settings.apiKey,
                prompt = prompt
            )
        }.getOrNull()

        return generated?.takeIf { it.isNotBlank() } ?: rawText
    }

    private fun buildExternalPrompt(
        assessmentTitle: String,
        rawText: String
    ): String = """
        Analiza este contenido académico para la evaluación "$assessmentTitle".
        Devuelve texto limpio y estructurado en español con este formato:
        1) Temas detectados (lista)
        2) Alcance evaluable probable
        3) Señales de dificultad y práctica
        4) Observaciones de repaso

        Contenido original:
        $rawText
    """.trimIndent()
}
