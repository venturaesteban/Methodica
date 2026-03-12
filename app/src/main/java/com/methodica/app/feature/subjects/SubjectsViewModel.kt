package com.methodica.app.feature.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.methodica.app.AppContainer
import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.usecase.subject.DeleteSubjectUseCase
import com.methodica.app.domain.usecase.subject.ObserveSubjectsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubjectsViewModel(
    private val observeSubjectsUseCase: ObserveSubjectsUseCase,
    private val deleteSubjectUseCase:   DeleteSubjectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectsUiState(isLoading = true))
    val uiState: StateFlow<SubjectsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSubjectsUseCase()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { subjects -> _uiState.update { it.copy(subjects = subjects, isLoading = false) } }
        }
    }

    fun onDeleteSubject(subject: Subject) {
        viewModelScope.launch { deleteSubjectUseCase(subject) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SubjectsViewModel(
                    observeSubjectsUseCase = container.observeSubjectsUseCase,
                    deleteSubjectUseCase   = container.deleteSubjectUseCase
                )
            }
        }
    }
}

