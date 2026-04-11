package com.methodica.app.feature.subjects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methodica.app.core.navigation.MethodicaDestination
import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.usecase.subject.DeleteSubjectUseCase
import com.methodica.app.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val subjectRepository: SubjectRepository,
    private val deleteSubjectUseCase:   DeleteSubjectUseCase
) : ViewModel() {

    private val academicYearId: Long = checkNotNull(
        savedStateHandle.get<Long>(MethodicaDestination.Subjects.ARG_ACADEMIC_YEAR_ID)
    )

    private val _uiState = MutableStateFlow(SubjectsUiState(isLoading = true))
    val uiState: StateFlow<SubjectsUiState> = _uiState.asStateFlow()

    init {
        observeSubjectsByAcademicYear()
    }

    private fun observeSubjectsByAcademicYear() {
        viewModelScope.launch {
            subjectRepository.observeSubjectsByAcademicYearId(academicYearId)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { subjects -> _uiState.update { it.copy(subjects = subjects, isLoading = false) } }
        }
    }

    fun onDeleteSubject(subject: Subject) {
        viewModelScope.launch { deleteSubjectUseCase(subject) }
    }
}

