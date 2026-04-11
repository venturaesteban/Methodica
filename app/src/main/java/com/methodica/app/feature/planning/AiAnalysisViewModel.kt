package com.methodica.app.feature.planning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methodica.app.core.navigation.MethodicaDestination
import com.methodica.app.domain.model.AiExecutionMode
import com.methodica.app.domain.model.TopicComplexityAnalysis
import com.methodica.app.domain.usecase.ai.AnalyzeAssessmentWithAiUseCase
import com.methodica.app.domain.usecase.ai.ApplyAiComplexityToTopicsUseCase
import com.methodica.app.domain.usecase.ai.GetLatestAiAnalysisForAssessmentUseCase
import com.methodica.app.domain.usecase.ai.ObserveAiProviderSettingsUseCase
import com.methodica.app.domain.usecase.ai.SaveAiAnalysisEditsUseCase
import com.methodica.app.domain.usecase.assessment.ObserveAllAssessmentsUseCase
import com.methodica.app.domain.usecase.assessmenttopic.ObserveAssessmentTopicsUseCase
import com.methodica.app.domain.usecase.planning.GenerateAssessmentPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AiAnalysisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeAiProviderSettingsUseCase: ObserveAiProviderSettingsUseCase,
    private val observeAllAssessmentsUseCase: ObserveAllAssessmentsUseCase,
    private val observeAssessmentTopicsUseCase: ObserveAssessmentTopicsUseCase,
    private val analyzeAssessmentWithAiUseCase: AnalyzeAssessmentWithAiUseCase,
    private val getLatestAiAnalysisForAssessmentUseCase: GetLatestAiAnalysisForAssessmentUseCase,
    private val saveAiAnalysisEditsUseCase: SaveAiAnalysisEditsUseCase,
    private val applyAiComplexityToTopicsUseCase: ApplyAiComplexityToTopicsUseCase,
    private val generateAssessmentPlanUseCase: GenerateAssessmentPlanUseCase
) : ViewModel() {

    private val assessmentIdArg: Long? =
        savedStateHandle.get<Long>(MethodicaDestination.AiAnalysis.ARG_ASSESSMENT_ID)
            ?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(AiAnalysisUiState())
    val uiState: StateFlow<AiAnalysisUiState> = _uiState.asStateFlow()

    private var topicsJob: Job? = null
    private var latestTopicsForAssessment: List<com.methodica.app.domain.model.Topic> = emptyList()

    init {
        observeAiSettings()
        observeAssessments()
    }

    private fun observeAiSettings() {
        viewModelScope.launch {
            observeAiProviderSettingsUseCase().collect { settings ->
                _uiState.update { state ->
                    val nextMode = if (!settings.isEnabledAndConfigured && state.aiExecutionMode == AiExecutionMode.EXTERNAL) {
                        AiExecutionMode.HEURISTIC
                    } else {
                        state.aiExecutionMode
                    }
                    state.copy(
                        canUseExternalAi = settings.isEnabledAndConfigured,
                        aiExecutionMode = nextMode
                    )
                }
            }
        }
    }

    private fun observeAssessments() {
        viewModelScope.launch {
            observeAllAssessmentsUseCase().collect { assessments ->
                val selected = _uiState.value.selectedAssessment
                val selectedAssessment = when {
                    selected != null && assessments.any { it.id == selected.id } -> selected
                    assessmentIdArg != null -> assessments.firstOrNull { it.id == assessmentIdArg }
                    else -> assessments.firstOrNull()
                }
                _uiState.update {
                    it.copy(
                        assessments = assessments,
                        selectedAssessment = selectedAssessment,
                        isLoading = false
                    )
                }
                selectedAssessment?.let { onSelectAssessment(it.id) }
            }
        }
    }

    fun onSelectAssessment(assessmentId: Long) {
        val assessment = _uiState.value.assessments.firstOrNull { it.id == assessmentId } ?: return
        _uiState.update {
            it.copy(
                selectedAssessment = assessment,
                error = null,
                infoMessage = null
            )
        }

        topicsJob?.cancel()
        topicsJob = viewModelScope.launch {
            observeAssessmentTopicsUseCase(assessmentId).collect { topics ->
                latestTopicsForAssessment = topics
            }
        }

        viewModelScope.launch {
            val latest = getLatestAiAnalysisForAssessmentUseCase(assessmentId)
            if (latest != null) {
                _uiState.update {
                    it.copy(
                        analysisId = latest.analysis.id,
                        estimatedScope = latest.scope.estimatedScope,
                        justification = latest.scope.justification,
                        confidence = latest.analysis.confidence,
                        requiresConfirmation = latest.analysis.requiresConfirmation,
                        topicEdits = latest.topicComplexities.map { item ->
                            EditableTopicComplexity(
                                id = item.id,
                                topicName = item.topicName,
                                isIncludedInScope = item.isIncludedInScope,
                                complexityLevel = item.complexityLevel,
                                recommendedHours = item.recommendedHours,
                                priority = item.priority,
                                requiresPractice = item.requiresPractice,
                                requiresSpacedReview = item.requiresSpacedReview,
                                rationale = item.rationale
                            )
                        }
                    )
                }
            }
        }
    }

    fun onSourceTextChange(value: String) {
        _uiState.update { it.copy(sourceText = value, error = null, infoMessage = null) }
    }

    fun onAiExecutionModeChange(mode: AiExecutionMode) {
        _uiState.update {
            if (mode == AiExecutionMode.EXTERNAL && !it.canUseExternalAi) {
                it.copy(error = "Configura primero tu IA externa en Ajustes")
            } else {
                it.copy(aiExecutionMode = mode, error = null)
            }
        }
    }

    fun onEstimatedScopeChange(value: String) {
        _uiState.update { it.copy(estimatedScope = value) }
    }

    fun onJustificationChange(value: String) {
        _uiState.update { it.copy(justification = value) }
    }

    fun onToggleIncluded(topicName: String) {
        _uiState.update { state ->
            state.copy(
                topicEdits = state.topicEdits.map { topic ->
                    if (topic.topicName == topicName) topic.copy(isIncludedInScope = !topic.isIncludedInScope)
                    else topic
                }
            )
        }
    }

    fun onComplexityChange(topicName: String, value: Int) {
        _uiState.update { state ->
            state.copy(
                topicEdits = state.topicEdits.map { topic ->
                    if (topic.topicName == topicName) topic.copy(complexityLevel = value.coerceIn(1, 5))
                    else topic
                }
            )
        }
    }

    fun onHoursChange(topicName: String, value: Int) {
        _uiState.update { state ->
            state.copy(
                topicEdits = state.topicEdits.map { topic ->
                    if (topic.topicName == topicName) topic.copy(recommendedHours = value.coerceAtLeast(1))
                    else topic
                }
            )
        }
    }

    fun onAnalyze() {
        val assessment = _uiState.value.selectedAssessment ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null, infoMessage = null) }
            val result = analyzeAssessmentWithAiUseCase(
                assessmentId = assessment.id,
                rawText = _uiState.value.sourceText,
                executionMode = _uiState.value.aiExecutionMode,
                sourceLabel = assessment.title
            )
            if (result.isSuccess) {
                val latest = getLatestAiAnalysisForAssessmentUseCase(assessment.id)
                if (latest != null) {
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            analysisId = latest.analysis.id,
                            estimatedScope = latest.scope.estimatedScope,
                            justification = latest.scope.justification,
                            confidence = latest.analysis.confidence,
                            requiresConfirmation = latest.analysis.requiresConfirmation,
                            topicEdits = latest.topicComplexities.map { item ->
                                EditableTopicComplexity(
                                    id = item.id,
                                    topicName = item.topicName,
                                    isIncludedInScope = item.isIncludedInScope,
                                    complexityLevel = item.complexityLevel,
                                    recommendedHours = item.recommendedHours,
                                    priority = item.priority,
                                    requiresPractice = item.requiresPractice,
                                    requiresSpacedReview = item.requiresSpacedReview,
                                    rationale = item.rationale
                                )
                            },
                            infoMessage = "Análisis IA generado. Revisa y edita antes de regenerar el plan."
                        )
                    }
                } else {
                    _uiState.update { it.copy(isAnalyzing = false, error = "No se pudo recuperar el análisis generado") }
                }
            } else {
                _uiState.update { it.copy(isAnalyzing = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun onSaveEdits() {
        val analysisId = _uiState.value.analysisId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingEdits = true, error = null, infoMessage = null) }
            val result = saveAiAnalysisEditsUseCase(
                analysisId = analysisId,
                estimatedScope = _uiState.value.estimatedScope,
                justification = _uiState.value.justification,
                topicComplexities = _uiState.value.topicEdits.map { it.toDomain(analysisId) }
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isSavingEdits = false,
                        requiresConfirmation = false,
                        infoMessage = "Cambios guardados"
                    )
                }
            } else {
                _uiState.update { it.copy(isSavingEdits = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun onApplyAndRegenerate() {
        val assessment = _uiState.value.selectedAssessment ?: return
        val analysisId = _uiState.value.analysisId ?: return

        if (_uiState.value.requiresConfirmation && _uiState.value.confidence < 0.75f) {
            _uiState.update {
                it.copy(
                    error = "La confianza del análisis es baja. Revisa alcance/temas y guarda edición antes de aplicar."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingAndRegenerating = true, error = null, infoMessage = null) }

            val saveResult = saveAiAnalysisEditsUseCase(
                analysisId = analysisId,
                estimatedScope = _uiState.value.estimatedScope,
                justification = _uiState.value.justification,
                topicComplexities = _uiState.value.topicEdits.map { it.toDomain(analysisId) }
            )
            if (saveResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isApplyingAndRegenerating = false,
                        error = saveResult.exceptionOrNull()?.message
                    )
                }
                return@launch
            }

            val applyResult = applyAiComplexityToTopicsUseCase(
                originalTopics = latestTopicsForAssessment,
                editedComplexities = _uiState.value.topicEdits.map { it.toDomain(analysisId) }
            )
            if (applyResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isApplyingAndRegenerating = false,
                        error = applyResult.exceptionOrNull()?.message
                    )
                }
                return@launch
            }

            val regenerateResult = generateAssessmentPlanUseCase(assessment.id)
            if (regenerateResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        isApplyingAndRegenerating = false,
                        requiresConfirmation = false,
                        infoMessage = "Plan regenerado con ajustes IA aplicados"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isApplyingAndRegenerating = false,
                        error = regenerateResult.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    private fun EditableTopicComplexity.toDomain(analysisId: Long): TopicComplexityAnalysis =
        TopicComplexityAnalysis(
            id = id,
            analysisId = analysisId,
            topicName = topicName,
            isIncludedInScope = isIncludedInScope,
            complexityLevel = complexityLevel,
            recommendedHours = recommendedHours,
            priority = priority,
            requiresPractice = requiresPractice,
            requiresSpacedReview = requiresSpacedReview,
            rationale = rationale
        )
}
