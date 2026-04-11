package com.methodica.app.feature.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.methodica.app.core.navigation.MethodicaDestination
import com.methodica.app.domain.repository.AcademicCatalogRepository
import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.usecase.academic.EnsureAcademicYearUseCase
import com.methodica.app.domain.usecase.academic.EnsureDegreeUseCase
import com.methodica.app.domain.usecase.subject.GetSubjectUseCase
import com.methodica.app.domain.usecase.subject.UpsertSubjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SubjectFormViewModel @Inject constructor(
    savedStateHandle:                 SavedStateHandle,
    private val academicCatalogRepository: AcademicCatalogRepository,
    private val getSubjectUseCase:  GetSubjectUseCase,
    private val ensureDegreeUseCase: EnsureDegreeUseCase,
    private val ensureAcademicYearUseCase: EnsureAcademicYearUseCase,
    private val upsertSubjectUseCase: UpsertSubjectUseCase
) : ViewModel() {

    private val subjectId: Long? =
        savedStateHandle.get<Long>(MethodicaDestination.SubjectForm.ARG_SUBJECT_ID)
            ?.takeIf { it != -1L }

    private val preselectedAcademicYearId: Long? =
        savedStateHandle.get<Long>(MethodicaDestination.SubjectForm.ARG_ACADEMIC_YEAR_ID)
            ?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(SubjectFormUiState())
    val uiState: StateFlow<SubjectFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (subjectId != null) {
                val subject = getSubjectUseCase(subjectId)
                subject?.let { s ->
                    _uiState.update {
                        it.copy(
                            name        = s.name,
                            colorHex    = s.colorHex,
                            description = s.description ?: "",
                            degreeName  = s.degreeName,
                            courseYear  = s.courseYear.toString()
                        )
                    }
                }
                return@launch
            }

            val initialAcademicYearId = preselectedAcademicYearId ?: return@launch
            val year = academicCatalogRepository.getAcademicYear(initialAcademicYearId) ?: return@launch
            val degree = academicCatalogRepository.getDegree(year.degreeId) ?: return@launch
            _uiState.update {
                it.copy(
                    degreeName = degree.name,
                    courseYear = year.yearNumber.toString()
                )
            }
        }
    }

    fun onNameChange(value: String)        = _uiState.update { it.copy(name = value, nameError = null) }
    fun onColorChange(color: String)       = _uiState.update { it.copy(colorHex = color) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onDegreeNameChange(value: String)  = _uiState.update { it.copy(degreeName = value, degreeNameError = null) }
    fun onCourseYearChange(value: String)  = _uiState.update { it.copy(courseYear = value, courseYearError = null) }

    fun onSave() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "El nombre no puede estar vacío") }
            return
        }
        if (state.degreeName.isBlank()) {
            _uiState.update { it.copy(degreeNameError = "La titulacion no puede estar vacía") }
            return
        }
        val courseYear = state.courseYear.toIntOrNull()
        if (courseYear == null || courseYear <= 0) {
            _uiState.update { it.copy(courseYearError = "El curso debe ser un numero mayor que 0") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val degreeResult = ensureDegreeUseCase(state.degreeName)
            if (degreeResult.isFailure) {
                _uiState.update { it.copy(isLoading = false, error = degreeResult.exceptionOrNull()?.message) }
                return@launch
            }
            val degreeId = degreeResult.getOrNull() ?: 0L

            val academicYearResult = ensureAcademicYearUseCase(degreeId, courseYear)
            if (academicYearResult.isFailure) {
                _uiState.update { it.copy(isLoading = false, error = academicYearResult.exceptionOrNull()?.message) }
                return@launch
            }
            val academicYearId = academicYearResult.getOrNull() ?: 0L

            val subject = Subject(
                id          = subjectId ?: 0L,
                name        = state.name.trim(),
                colorHex    = state.colorHex,
                description = state.description.ifBlank { null },
                academicYearId = academicYearId,
                degreeId = degreeId,
                degreeName  = state.degreeName.trim(),
                courseYear  = courseYear
            )
            val result = upsertSubjectUseCase(subject)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaved = true, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

}
