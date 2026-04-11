package com.methodica.app.feature.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.methodica.app.core.navigation.MethodicaDestination
import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.usecase.topic.GetTopicUseCase
import com.methodica.app.domain.usecase.topic.UpsertTopicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TopicFormViewModel @Inject constructor(
    savedStateHandle:               SavedStateHandle,
    private val getTopicUseCase:   GetTopicUseCase,
    private val upsertTopicUseCase: UpsertTopicUseCase
) : ViewModel() {

    private val subjectId: Long = checkNotNull(
        savedStateHandle[MethodicaDestination.TopicForm.ARG_SUBJECT_ID]
    )

    private val topicId: Long? =
        savedStateHandle.get<Long>(MethodicaDestination.TopicForm.ARG_TOPIC_ID)
            ?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(TopicFormUiState())
    val uiState: StateFlow<TopicFormUiState> = _uiState.asStateFlow()

    init {
        if (topicId != null) {
            viewModelScope.launch {
                val topic = getTopicUseCase(topicId)
                topic?.let { t ->
                    _uiState.update {
                        it.copy(
                            name           = t.name,
                            difficulty     = t.difficulty,
                            estimatedHours = t.estimatedHours,
                            order          = t.order
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String)           = _uiState.update { it.copy(name = value, nameError = null) }
    fun onDifficultyChange(value: Int)        = _uiState.update { it.copy(difficulty = value) }
    fun onEstimatedHoursChange(value: Int)    = _uiState.update { it.copy(estimatedHours = value, estimatedHoursError = null) }
    fun onOrderChange(value: Int)             = _uiState.update { it.copy(order = value) }

    fun onSave() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "El nombre no puede estar vacío") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val topic = Topic(
                id             = topicId ?: 0L,
                subjectId      = subjectId,
                name           = state.name.trim(),
                difficulty     = state.difficulty,
                estimatedHours = state.estimatedHours,
                order          = state.order
            )
            val result = upsertTopicUseCase(topic)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaved = true, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

}
