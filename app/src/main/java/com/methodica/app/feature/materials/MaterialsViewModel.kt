package com.methodica.app.feature.materials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methodica.app.domain.model.Material
import com.methodica.app.domain.model.MaterialType
import com.methodica.app.domain.usecase.material.DeleteMaterialUseCase
import com.methodica.app.domain.usecase.material.ObserveAllMaterialsUseCase
import com.methodica.app.domain.usecase.subject.ObserveSubjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MaterialsViewModel @Inject constructor(
    observeSubjectsUseCase: ObserveSubjectsUseCase,
    observeAllMaterialsUseCase: ObserveAllMaterialsUseCase,
    private val deleteMaterialUseCase: DeleteMaterialUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaterialsUiState())
    val uiState: StateFlow<MaterialsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                observeSubjectsUseCase(),
                observeAllMaterialsUseCase()
            ) { subjects, materials -> subjects to materials }
                .collect { (subjects, materials) ->
                    _uiState.update { state ->
                        val validDegreeId = state.selectedDegreeId?.takeIf { degreeId ->
                            subjects.any { it.degreeId == degreeId }
                        }
                        val validCourseYear = state.selectedCourseYear?.takeIf { courseYear ->
                            subjects.any { it.courseYear == courseYear && (validDegreeId == null || it.degreeId == validDegreeId) }
                        }
                        val validSubjectId = state.selectedSubjectId?.takeIf { selectedId ->
                            subjects.any {
                                it.id == selectedId &&
                                    (validDegreeId == null || it.degreeId == validDegreeId) &&
                                    (validCourseYear == null || it.courseYear == validCourseYear)
                            }
                        }
                        state.copy(
                            isLoading = false,
                            subjects = subjects,
                            materials = materials,
                            selectedDegreeId = validDegreeId,
                            selectedCourseYear = validCourseYear,
                            selectedSubjectId = validSubjectId
                        )
                    }
                }
        }
    }

    fun onFilterDegree(degreeId: Long?) {
        _uiState.update { state ->
            val selectedCourseYear = state.selectedCourseYear?.takeIf { courseYear ->
                state.subjects.any { it.courseYear == courseYear && (degreeId == null || it.degreeId == degreeId) }
            }
            val selectedSubjectId = state.selectedSubjectId?.takeIf { subjectId ->
                state.subjects.any {
                    it.id == subjectId &&
                        (degreeId == null || it.degreeId == degreeId) &&
                        (selectedCourseYear == null || it.courseYear == selectedCourseYear)
                }
            }
            state.copy(
                selectedDegreeId = degreeId,
                selectedCourseYear = selectedCourseYear,
                selectedSubjectId = selectedSubjectId
            )
        }
    }

    fun onFilterCourseYear(courseYear: Int?) {
        _uiState.update { state ->
            val selectedSubjectId = state.selectedSubjectId?.takeIf { subjectId ->
                state.subjects.any {
                    it.id == subjectId &&
                        (state.selectedDegreeId == null || it.degreeId == state.selectedDegreeId) &&
                        (courseYear == null || it.courseYear == courseYear)
                }
            }
            state.copy(selectedCourseYear = courseYear, selectedSubjectId = selectedSubjectId)
        }
    }

    fun onFilterSubject(subjectId: Long?) {
        _uiState.update { state ->
            val subject = state.subjects.firstOrNull { it.id == subjectId }
            state.copy(
                selectedDegreeId = subject?.degreeId ?: state.selectedDegreeId,
                selectedCourseYear = subject?.courseYear ?: state.selectedCourseYear,
                selectedSubjectId = subjectId
            )
        }
    }

    fun onFilterType(type: MaterialType?) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun onDeleteMaterial(material: Material) {
        viewModelScope.launch {
            deleteMaterialUseCase(material)
        }
    }
}
