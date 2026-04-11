package com.methodica.app.feature.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.methodica.app.core.navigation.MethodicaDestination
import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.AssessmentType
import com.methodica.app.domain.usecase.assessment.GetAssessmentUseCase
import com.methodica.app.domain.usecase.assessment.UpsertAssessmentUseCase
import com.methodica.app.domain.usecase.assessmenttopic.ObserveAssessmentTopicIdsUseCase
import com.methodica.app.domain.usecase.assessmenttopic.ReplaceAssessmentTopicsUseCase
import com.methodica.app.domain.usecase.topic.ObserveTopicsBySubjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AssessmentFormViewModel @Inject constructor(
    savedStateHandle:                             SavedStateHandle,
    private val getAssessmentUseCase:             GetAssessmentUseCase,
    private val upsertAssessmentUseCase:          UpsertAssessmentUseCase,
    private val observeTopicsBySubjectUseCase:    ObserveTopicsBySubjectUseCase,
    private val observeAssessmentTopicIdsUseCase: ObserveAssessmentTopicIdsUseCase,
    private val replaceAssessmentTopicsUseCase:   ReplaceAssessmentTopicsUseCase
) : ViewModel() {

    private val subjectId: Long = checkNotNull(
        savedStateHandle[MethodicaDestination.AssessmentForm.ARG_SUBJECT_ID]
    )

    private val assessmentId: Long? =
        savedStateHandle.get<Long>(MethodicaDestination.AssessmentForm.ARG_ASSESSMENT_ID)
            ?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(AssessmentFormUiState())
    val uiState: StateFlow<AssessmentFormUiState> = _uiState.asStateFlow()

    init {
        // Cargar topics de la asignatura
        viewModelScope.launch {
            observeTopicsBySubjectUseCase(subjectId).collect { topics ->
                _uiState.update { it.copy(availableTopics = topics) }
            }
        }

        if (assessmentId != null) {
            viewModelScope.launch {
                val assessment = getAssessmentUseCase(assessmentId)
                assessment?.let { a ->
                    _uiState.update {
                        it.copy(
                            title  = a.title,
                            type   = a.type,
                            date   = a.date,
                            weight = a.weight?.toString() ?: "",
                            notes  = a.notes ?: ""
                        )
                    }
                }
            }
            // Cargar topics asociados existentes
            viewModelScope.launch {
                observeAssessmentTopicIdsUseCase(assessmentId).collect { ids ->
                    _uiState.update { it.copy(selectedTopicIds = ids.toSet()) }
                }
            }
        }
    }

    fun onTitleChange(value: String)  = _uiState.update { it.copy(title = value, titleError = null) }
    fun onTypeChange(value: AssessmentType) = _uiState.update { it.copy(type = value) }
    fun onDateChange(value: Long)     = _uiState.update { it.copy(date = value) }
    fun onWeightChange(value: String) = _uiState.update { it.copy(weight = value, weightError = null) }
    fun onNotesChange(value: String)  = _uiState.update { it.copy(notes = value) }

    fun onToggleTopic(topicId: Long) {
        _uiState.update { state ->
            val updated = if (topicId in state.selectedTopicIds)
                state.selectedTopicIds - topicId
            else
                state.selectedTopicIds + topicId
            state.copy(selectedTopicIds = updated)
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "El título no puede estar vacío") }
            return
        }
        val weight = state.weight.let { w ->
            if (w.isBlank()) null
            else w.toIntOrNull().also { parsed ->
                if (parsed == null || parsed !in 0..100) {
                    _uiState.update { it.copy(weightError = "El peso debe ser un número entre 0 y 100") }
                    return
                }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val assessment = Assessment(
                id        = assessmentId ?: 0L,
                subjectId = subjectId,
                type      = state.type,
                title     = state.title.trim(),
                date      = state.date,
                weight    = weight,
                notes     = state.notes.ifBlank { null }
            )
            val result = upsertAssessmentUseCase(assessment)
            if (result.isSuccess) {
                val savedId = result.getOrNull() ?: assessmentId ?: 0L
                if (savedId > 0L) {
                    replaceAssessmentTopicsUseCase(savedId, state.selectedTopicIds.toList())
                }
                _uiState.update { it.copy(isSaved = true, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

}
