package com.methodica.app.feature.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methodica.app.domain.ai.model.AiPlanningInsight
import com.methodica.app.domain.ai.model.ComplexityAnalysis
import com.methodica.app.domain.ai.model.ExamScopeInference
import com.methodica.app.domain.ai.model.PlanningRecommendation
import com.methodica.app.domain.ai.workflow.AiWorkflowCoordinator
import com.methodica.app.domain.ai.workflow.AnalyzeAssessmentRequest
import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.AiExecutionMode
import com.methodica.app.domain.usecase.assessment.ObserveAllAssessmentsUseCase
import com.methodica.app.domain.usecase.assessmenttopic.ObserveAssessmentTopicsUseCase
import com.methodica.app.domain.usecase.planning.GenerateAssessmentPlanUseCase
import com.methodica.app.domain.usecase.subject.ObserveSubjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlanningViewModel @Inject constructor(
    private val observeSubjectsUseCase: ObserveSubjectsUseCase,
    private val observeAllAssessmentsUseCase: ObserveAllAssessmentsUseCase,
    private val observeAssessmentTopicsUseCase: ObserveAssessmentTopicsUseCase,
    private val aiWorkflowCoordinator: AiWorkflowCoordinator,
    private val generateAssessmentPlanUseCase: GenerateAssessmentPlanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanningUiState())
    val uiState: StateFlow<PlanningUiState> = _uiState.asStateFlow()

    private var topicsJob: Job? = null

    init {
        viewModelScope.launch {
            observeSubjectsUseCase().collect { subjects ->
                _uiState.update { it.copy(subjects = subjects, isLoading = false) }
            }
        }
        viewModelScope.launch {
            aiWorkflowCoordinator.observeCapabilities().collect { capability ->
                _uiState.update { state ->
                    val currentMode = if (!capability.canUseExternalAi && state.aiExecutionMode == AiExecutionMode.EXTERNAL) {
                        AiExecutionMode.HEURISTIC
                    } else {
                        state.aiExecutionMode
                    }
                    state.copy(
                        canUseExternalAi = capability.canUseExternalAi,
                        localModelsReady = capability.localModelsReady,
                        runtimeMessage = capability.runtimeMessage,
                        aiExecutionMode = currentMode
                    )
                }
            }
        }
        viewModelScope.launch {
            observeAllAssessmentsUseCase().collect { assessments ->
                val selectedId = _uiState.value.selectedAssessment?.id
                val selectedAssessment = selectedId?.let { id -> assessments.firstOrNull { it.id == id } }
                val selectionRemoved = selectedId != null && selectedAssessment == null

                if (selectionRemoved) {
                    topicsJob?.cancel()
                    topicsJob = null
                }

                _uiState.update {
                    it.copy(
                        assessments = assessments,
                        selectedAssessment = selectedAssessment,
                        linkedTopics = if (selectionRemoved) emptyList() else it.linkedTopics,
                        aiInputText = if (selectionRemoved) "" else it.aiInputText,
                        lastAiInsight = if (selectionRemoved) null else it.lastAiInsight,
                        aiError = if (selectionRemoved) null else it.aiError,
                        lastResult = if (selectionRemoved) null else it.lastResult,
                        error = if (selectionRemoved) null else it.error,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSelectAssessment(assessment: Assessment) {
        _uiState.update {
            it.copy(
                selectedAssessment = assessment,
                lastResult = null,
                lastAiInsight = null,
                error = null,
                aiError = null
            )
        }
        topicsJob?.cancel()
        topicsJob = viewModelScope.launch {
            observeAssessmentTopicsUseCase(assessment.id).collect { topics ->
                _uiState.update { it.copy(linkedTopics = topics) }
            }
        }
    }

    fun onAiInputChange(value: String) {
        _uiState.update { it.copy(aiInputText = value, aiError = null) }
    }

    fun onAiExecutionModeChange(mode: AiExecutionMode) {
        _uiState.update {
            if (mode == AiExecutionMode.EXTERNAL && !it.canUseExternalAi) {
                it.copy(aiError = "Configura primero tu IA externa en Ajustes")
            } else {
                it.copy(aiExecutionMode = mode, aiError = null)
            }
        }
    }

    fun onAnalyzeWithAi() {
        val assessment = _uiState.value.selectedAssessment ?: return
        val text = _uiState.value.aiInputText

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingAi = true, aiError = null) }
            val result = aiWorkflowCoordinator.analyzeAssessment(
                AnalyzeAssessmentRequest(
                    assessmentId = assessment.id,
                    rawText = text,
                    executionMode = _uiState.value.aiExecutionMode,
                    sourceLabel = assessment.title
                )
            )
            if (result.isSuccess) {
                val stored = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isAnalyzingAi = false,
                        lastAiInsight = AiPlanningInsight(
                            scopeInference = ExamScopeInference(
                                estimatedScope = stored.scope.estimatedScope,
                                includedTopicNames = stored.topicComplexities.filter { item -> item.isIncludedInScope }.map { item -> item.topicName },
                                justification = stored.scope.justification,
                                confidence = stored.scope.confidence,
                                requiresConfirmation = stored.scope.requiresUserConfirmation
                            ),
                            complexityAnalysis = ComplexityAnalysis(
                                topics = emptyList(),
                                overallComplexity = stored.topicComplexities.map { item -> item.complexityLevel }.average().toInt().coerceAtLeast(1),
                                confidence = stored.analysis.confidence
                            ),
                            recommendation = PlanningRecommendation(
                                recommendedTopicOrder = stored.topicComplexities.sortedByDescending { item -> item.priority }.map { item -> item.topicName },
                                extraReviewTopics = stored.topicComplexities.filter { item -> item.requiresSpacedReview }.map { item -> item.topicName },
                                warnings = emptyList(),
                                confidence = stored.analysis.confidence
                            ),
                            summary = stored.analysis.summary,
                            confidence = stored.analysis.confidence,
                            requiresConfirmation = stored.analysis.requiresConfirmation
                        ),
                        aiError = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isAnalyzingAi = false,
                        aiError = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    fun onGenerate() {
        val assessment = _uiState.value.selectedAssessment ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            val result = generateAssessmentPlanUseCase(assessment.id)
            if (result.isSuccess) {
                _uiState.update { it.copy(isGenerating = false, lastResult = result.getOrNull()) }
            } else {
                _uiState.update { it.copy(isGenerating = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }
}
