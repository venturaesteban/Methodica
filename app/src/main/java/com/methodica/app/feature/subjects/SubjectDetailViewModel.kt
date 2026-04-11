package com.methodica.app.feature.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.methodica.app.core.navigation.MethodicaDestination
import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.usecase.assessment.DeleteAssessmentUseCase
import com.methodica.app.domain.usecase.ai.EstimateTopicComplexityUseCase
import com.methodica.app.domain.usecase.assessment.ObserveAssessmentsBySubjectUseCase
import com.methodica.app.domain.usecase.subject.DeleteSubjectUseCase
import com.methodica.app.domain.usecase.subject.GetSubjectUseCase
import com.methodica.app.domain.usecase.topic.DeleteTopicUseCase
import com.methodica.app.domain.usecase.topic.ObserveTopicsBySubjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SubjectDetailViewModel @Inject constructor(
    savedStateHandle:                           SavedStateHandle,
    private val getSubjectUseCase:              GetSubjectUseCase,
    private val observeTopicsUseCase:           ObserveTopicsBySubjectUseCase,
    private val observeAssessmentsUseCase:      ObserveAssessmentsBySubjectUseCase,
    private val deleteSubjectUseCase:           DeleteSubjectUseCase,
    private val deleteTopicUseCase:             DeleteTopicUseCase,
    private val deleteAssessmentUseCase:        DeleteAssessmentUseCase,
    private val estimateTopicComplexityUseCase: EstimateTopicComplexityUseCase
) : ViewModel() {

    private val subjectId: Long = checkNotNull(
        savedStateHandle[MethodicaDestination.SubjectDetail.ARG_SUBJECT_ID]
    )

    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    init {
        loadSubject()
        observeCollections()
    }

    private fun loadSubject() {
        viewModelScope.launch {
            val subject = getSubjectUseCase(subjectId)
            _uiState.update { it.copy(subject = subject, isLoading = false) }
        }
    }

    private fun observeCollections() {
        viewModelScope.launch {
            combine(
                observeTopicsUseCase(subjectId),
                observeAssessmentsUseCase(subjectId)
            ) { topics, assessments -> topics to assessments }
                .collect { (topics, assessments) ->
                    _uiState.update { it.copy(topics = topics, assessments = assessments) }
                }
        }
    }

    fun onDeleteSubject(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.value.subject?.let { subject ->
                deleteSubjectUseCase(subject)
                onDeleted()
            }
        }
    }

    fun onDeleteTopic(topic: Topic) {
        viewModelScope.launch { deleteTopicUseCase(topic) }
    }

    fun onDeleteAssessment(assessment: Assessment) {
        viewModelScope.launch { deleteAssessmentUseCase(assessment) }
    }

    fun onEstimateTopicWithAi(topic: Topic) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    estimatingTopicId = topic.id,
                    topicEstimationMessage = null,
                    error = null
                )
            }

            val result = estimateTopicComplexityUseCase(topic.id)
            if (result.isSuccess) {
                val estimate = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        estimatingTopicId = null,
                        topicEstimationMessage = "Estimación IA aplicada (${estimate.source})."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        estimatingTopicId = null,
                        topicEstimationMessage = null,
                        error = result.exceptionOrNull()?.message ?: "No se pudo estimar el tema"
                    )
                }
            }
        }
    }
}
