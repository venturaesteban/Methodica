package com.methodica.app.feature.degree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methodica.app.domain.model.DegreeStatus
import com.methodica.app.domain.repository.AcademicCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DegreesViewModel @Inject constructor(
    private val catalogRepository: AcademicCatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DegreesUiState())
    val uiState: StateFlow<DegreesUiState> = _uiState.asStateFlow()

    init {
        observeDegrees()
    }

    private fun observeDegrees() {
        viewModelScope.launch {
            catalogRepository.observeDegrees().collect { degrees ->
                _uiState.update { it.copy(degrees = degrees, isLoading = false) }
            }
        }
    }

    fun deleteDegree(degreeId: Long) {
        viewModelScope.launch {
            catalogRepository.deleteDegree(degreeId)
        }
    }

    fun createNextAcademicYearForDegree(degreeId: Long) {
        viewModelScope.launch {
            val years = catalogRepository.observeAcademicYears(degreeId).first()
            val nextYear = (years.maxOfOrNull { it.yearNumber } ?: 0) + 1
            catalogRepository.ensureAcademicYear(degreeId, nextYear)
        }
    }

    fun updateDegreeStatus(degreeId: Long, status: DegreeStatus) {
        viewModelScope.launch {
            catalogRepository.updateDegreeStatus(degreeId, status)
        }
    }
}

