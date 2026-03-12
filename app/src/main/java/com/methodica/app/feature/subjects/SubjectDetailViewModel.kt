package com.methodica.app.feature.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.methodica.app.AppContainer
import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.usecase.assessment.DeleteAssessmentUseCase
import com.methodica.app.domain.usecase.assessment.ObserveAssessmentsBySubjectUseCase
import com.methodica.app.domain.usecase.subject.DeleteSubjectUseCase
import com.methodica.app.domain.usecase.subject.GetSubjectUseCase
import com.methodica.app.domain.usecase.topic.DeleteTopicUseCase
import com.methodica.app.domain.usecase.topic.ObserveTopicsBySubjectUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubjectDetailViewModel(
    private val subjectId:                      Long,
    private val getSubjectUseCase:              GetSubjectUseCase,
    private val observeTopicsUseCase:           ObserveTopicsBySubjectUseCase,
    private val observeAssessmentsUseCase:      ObserveAssessmentsBySubjectUseCase,
    private val deleteSubjectUseCase:           DeleteSubjectUseCase,
    private val deleteTopicUseCase:             DeleteTopicUseCase,
    private val deleteAssessmentUseCase:        DeleteAssessmentUseCase
) : ViewModel() {

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

    companion object {
        fun factory(subjectId: Long, container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    SubjectDetailViewModel(
                        subjectId                 = subjectId,
                        getSubjectUseCase         = container.getSubjectUseCase,
                        observeTopicsUseCase      = container.observeTopicsBySubjectUseCase,
                        observeAssessmentsUseCase = container.observeAssessmentsBySubjectUseCase,
                        deleteSubjectUseCase      = container.deleteSubjectUseCase,
                        deleteTopicUseCase        = container.deleteTopicUseCase,
                        deleteAssessmentUseCase   = container.deleteAssessmentUseCase
                    )
                }
            }
    }
}
