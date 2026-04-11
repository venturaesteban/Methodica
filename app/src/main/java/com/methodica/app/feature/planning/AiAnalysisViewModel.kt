package com.methodica.app.feature.planning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methodica.app.core.navigation.MethodicaDestination
import com.methodica.app.domain.ai.workflow.AiWorkflowCoordinator
import com.methodica.app.domain.ai.workflow.AnalyzeAssessmentRequest
import com.methodica.app.domain.ai.workflow.ApplyAnalysisEditsRequest
import com.methodica.app.domain.model.AiExecutionMode
import com.methodica.app.domain.model.TopicComplexityAnalysis
import com.methodica.app.domain.usecase.assessment.ObserveAllAssessmentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AiAnalysisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeAllAssessmentsUseCase: ObserveAllAssessmentsUseCase,
    private val aiWorkflowCoordinator: AiWorkflowCoordinator
) : ViewModel() {

    private val assessmentIdArg: Long? =
        savedStateHandle.get<Long>(MethodicaDestination.AiAnalysis.ARG_ASSESSMENT_ID)
            ?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(AiAnalysisUiState())
    val uiState: StateFlow<AiAnalysisUiState> = _uiState.asStateFlow()

    init {
        observeCapabilities()
        observeAssessments()
    }

    private fun observeCapabilities() {
        viewModelScope.launch {
            aiWorkflowCoordinator.observeCapabilities().collect { capability ->
                _uiState.update { state ->
                    val nextMode = if (!capability.canUseExternalAi && state.aiExecutionMode == AiExecutionMode.EXTERNAL) {
                        AiExecutionMode.HEURISTIC
                    } else {
                        state.aiExecutionMode
                    }
                    state.copy(
                        canUseExternalAi = capability.canUseExternalAi,
                        localModelsReady = capability.localModelsReady,
                        runtimeMessage = capability.runtimeMessage,
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

        viewModelScope.launch {
            aiWorkflowCoordinator.getLatestAnalysis(assessmentId)?.let { latest ->
                _uiState.update { state ->
                    state.copy(
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
                    if (topic.topicName == topicName) topic.copy(isIncludedInScope = !topic.isIncludedInScope) else topic
                }
            )
        }
    }

    fun onComplexityChange(topicName: String, value: Int) {
        _uiState.update { state ->
            state.copy(
                topicEdits = state.topicEdits.map { topic ->
                    if (topic.topicName == topicName) topic.copy(complexityLevel = value.coerceIn(1, 5)) else topic
                }
            )
        }
    }

    fun onHoursChange(topicName: String, value: Int) {
        _uiState.update { state ->
            state.copy(
                topicEdits = state.topicEdits.map { topic ->
                    if (topic.topicName == topicName) topic.copy(recommendedHours = value.coerceAtLeast(1)) else topic
                }
            )
        }
    }

    fun onAnalyze() {
        val assessment = _uiState.value.selectedAssessment ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null, infoMessage = null) }
            val result = aiWorkflowCoordinator.analyzeAssessment(
                AnalyzeAssessmentRequest(
                    assessmentId = assessment.id,
                    rawText = _uiState.value.sourceText,
                    executionMode = _uiState.value.aiExecutionMode,
                    sourceLabel = assessment.title
                )
            )
            if (result.isSuccess) {
                onSelectAssessment(assessment.id)
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        infoMessage = "Análisis IA generado. Revisa y edita antes de regenerar el plan."
                    )
                }
            } else {
                _uiState.update { it.copy(isAnalyzing = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun onSaveEdits() {
        val request = currentApplyRequest() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingEdits = true, error = null, infoMessage = null) }
            val result = aiWorkflowCoordinator.saveAnalysisEdits(request)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isSavingEdits = false, requiresConfirmation = false, infoMessage = "Cambios guardados")
                }
            } else {
                _uiState.update { it.copy(isSavingEdits = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun onApplyAndRegenerate() {
        val request = currentApplyRequest() ?: return

        if (_uiState.value.requiresConfirmation && _uiState.value.confidence < 0.75f) {
            _uiState.update {
                it.copy(error = "La confianza del análisis es baja. Revisa alcance/temas y guarda edición antes de aplicar.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingAndRegenerating = true, error = null, infoMessage = null) }
            val result = aiWorkflowCoordinator.applyEditsAndRegenerate(request)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isApplyingAndRegenerating = false,
                        requiresConfirmation = false,
                        infoMessage = "Plan regenerado con ajustes IA aplicados"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isApplyingAndRegenerating = false, error = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    private fun currentApplyRequest(): ApplyAnalysisEditsRequest? {
        val assessment = _uiState.value.selectedAssessment ?: return null
        val analysisId = _uiState.value.analysisId ?: return null
        return ApplyAnalysisEditsRequest(
            assessmentId = assessment.id,
            analysisId = analysisId,
            estimatedScope = _uiState.value.estimatedScope,
            justification = _uiState.value.justification,
            topicComplexities = _uiState.value.topicEdits.map { it.toDomain(analysisId) }
        )
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
