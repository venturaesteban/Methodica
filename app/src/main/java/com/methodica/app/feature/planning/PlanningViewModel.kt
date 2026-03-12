package com.methodica.app.feature.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.methodica.app.AppContainer
import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.usecase.assessment.ObserveAllAssessmentsUseCase
import com.methodica.app.domain.usecase.assessmenttopic.ObserveAssessmentTopicsUseCase
import com.methodica.app.domain.usecase.planning.GenerateAssessmentPlanUseCase
import com.methodica.app.domain.usecase.subject.ObserveSubjectsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlanningViewModel(
    private val observeSubjectsUseCase:          ObserveSubjectsUseCase,
    private val observeAllAssessmentsUseCase:    ObserveAllAssessmentsUseCase,
    private val observeAssessmentTopicsUseCase:  ObserveAssessmentTopicsUseCase,
    private val generateAssessmentPlanUseCase:   GenerateAssessmentPlanUseCase
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
            observeAllAssessmentsUseCase().collect { assessments ->
                _uiState.update { it.copy(assessments = assessments, isLoading = false) }
            }
        }
    }

    fun onSelectAssessment(assessment: Assessment) {
        _uiState.update { it.copy(selectedAssessment = assessment, lastResult = null, error = null) }
        topicsJob?.cancel()
        topicsJob = viewModelScope.launch {
            observeAssessmentTopicsUseCase(assessment.id).collect { topics ->
                _uiState.update { it.copy(linkedTopics = topics) }
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

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PlanningViewModel(
                    observeSubjectsUseCase         = container.observeSubjectsUseCase,
                    observeAllAssessmentsUseCase   = container.observeAllAssessmentsUseCase,
                    observeAssessmentTopicsUseCase = container.observeAssessmentTopicsUseCase,
                    generateAssessmentPlanUseCase  = container.generateAssessmentPlanUseCase
                )
            }
        }
    }
}
