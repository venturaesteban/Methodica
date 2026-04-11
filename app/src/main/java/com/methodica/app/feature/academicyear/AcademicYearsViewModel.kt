package com.methodica.app.feature.academicyear

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methodica.app.core.navigation.MethodicaDestination
import com.methodica.app.domain.repository.AcademicCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AcademicYearsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: AcademicCatalogRepository
) : ViewModel() {

    private val degreeId: Long = checkNotNull(
        savedStateHandle.get<Long>(MethodicaDestination.AcademicYears.ARG_DEGREE_ID)
    )

    private val _uiState = MutableStateFlow(AcademicYearsUiState(isLoading = true))
    val uiState: StateFlow<AcademicYearsUiState> = _uiState.asStateFlow()

    init {
        loadDegreeAndAcademicYears()
    }

    private fun loadDegreeAndAcademicYears() {
        viewModelScope.launch {
            // Cargar el grado
            val degree = catalogRepository.getDegree(degreeId)
            _uiState.update { it.copy(degree = degree) }

            // Observar los años académicos
            catalogRepository.observeAcademicYears(degreeId).collect { academicYears ->
                _uiState.update { it.copy(academicYears = academicYears, isLoading = false) }
            }
        }
    }

    fun createNextAcademicYear() {
        viewModelScope.launch {
            val nextYear = (_uiState.value.academicYears.maxOfOrNull { it.yearNumber } ?: 0) + 1
            catalogRepository.ensureAcademicYear(degreeId, nextYear)
        }
    }

    fun deleteAcademicYear(academicYearId: Long) {
        viewModelScope.launch {
            catalogRepository.deleteAcademicYear(academicYearId)
        }
    }
}


