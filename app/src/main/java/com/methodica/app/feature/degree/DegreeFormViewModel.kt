package com.methodica.app.feature.degree

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methodica.app.domain.model.Degree
import com.methodica.app.domain.model.DegreeStatus
import com.methodica.app.domain.repository.AcademicCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DegreeFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: AcademicCatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DegreeFormUiState())
    val uiState: StateFlow<DegreeFormUiState> = _uiState.asStateFlow()

    private val degreeId: Long? = savedStateHandle["degreeId"]

    init {
        if (degreeId != null && degreeId != -1L) {
            loadDegree()
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadDegree() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentId = degreeId ?: return@launch
            val degree = catalogRepository.getDegree(currentId)
            _uiState.update { it.copy(degree = degree, isLoading = false) }
        }
    }

    fun createDegree(name: String, description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, isSaved = false, error = null) }
            try {
                catalogRepository.upsertDegree(
                    Degree(name = name, description = description.ifBlank { null })
                )
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSaving = false, 
                        isSaved = false,
                        error = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    fun updateDegree(degreeId: Long, name: String, description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, isSaved = false, error = null) }
            try {
                catalogRepository.upsertDegree(
                    Degree(
                        id = degreeId,
                        name = name,
                        description = description.ifBlank { null },
                        status = uiState.value.degree?.status ?: DegreeStatus.ACTIVE
                    )
                )
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSaving = false, 
                        isSaved = false,
                        error = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }
}

