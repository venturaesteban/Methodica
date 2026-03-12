package com.methodica.app.feature.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.methodica.app.AppContainer
import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.usecase.subject.GetSubjectUseCase
import com.methodica.app.domain.usecase.subject.UpsertSubjectUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubjectFormViewModel(
    private val subjectId:          Long?,
    private val getSubjectUseCase:  GetSubjectUseCase,
    private val upsertSubjectUseCase: UpsertSubjectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectFormUiState())
    val uiState: StateFlow<SubjectFormUiState> = _uiState.asStateFlow()

    init {
        if (subjectId != null) {
            viewModelScope.launch {
                val subject = getSubjectUseCase(subjectId)
                subject?.let { s ->
                    _uiState.update {
                        it.copy(
                            name        = s.name,
                            colorHex    = s.colorHex,
                            description = s.description ?: ""
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String)        = _uiState.update { it.copy(name = value, nameError = null) }
    fun onColorChange(color: String)       = _uiState.update { it.copy(colorHex = color) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun onSave() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "El nombre no puede estar vacío") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val subject = Subject(
                id          = subjectId ?: 0L,
                name        = state.name.trim(),
                colorHex    = state.colorHex,
                description = state.description.ifBlank { null }
            )
            val result = upsertSubjectUseCase(subject)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaved = true, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    companion object {
        fun factory(subjectId: Long?, container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    SubjectFormViewModel(
                        subjectId           = subjectId,
                        getSubjectUseCase   = container.getSubjectUseCase,
                        upsertSubjectUseCase = container.upsertSubjectUseCase
                    )
                }
            }
    }
}
