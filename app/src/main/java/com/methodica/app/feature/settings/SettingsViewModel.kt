package com.methodica.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.methodica.app.AppContainer
import com.methodica.app.domain.model.PlanningSettings
import com.methodica.app.domain.usecase.planning.ObservePlanningSettingsUseCase
import com.methodica.app.domain.usecase.planning.SavePlanningSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class SettingsViewModel(
    private val observePlanningSettingsUseCase: ObservePlanningSettingsUseCase,
    private val savePlanningSettingsUseCase:    SavePlanningSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observePlanningSettingsUseCase().collect { settings ->
                _uiState.update {
                    it.copy(
                        isLoading            = false,
                        availableHoursPerDay = settings.availableHoursPerDay,
                        unavailableWeekdays  = settings.unavailableWeekdays,
                        bufferDaysBeforeExam = settings.bufferDaysBeforeExam,
                        finalReviewDays      = settings.finalReviewDays
                    )
                }
            }
        }
    }

    fun onHoursPerDayChange(value: Int) =
        _uiState.update { it.copy(availableHoursPerDay = value.coerceIn(1, 16), error = null) }

    fun onToggleWeekday(day: DayOfWeek) {
        _uiState.update { state ->
            val updated = if (day in state.unavailableWeekdays)
                state.unavailableWeekdays - day
            else
                state.unavailableWeekdays + day
            state.copy(unavailableWeekdays = updated, error = null)
        }
    }

    fun onBufferDaysChange(value: Int) =
        _uiState.update { it.copy(bufferDaysBeforeExam = value.coerceAtLeast(0), error = null) }

    fun onFinalReviewDaysChange(value: Int) =
        _uiState.update { it.copy(finalReviewDays = value.coerceAtLeast(0), error = null) }

    fun onSave() {
        val state = _uiState.value
        viewModelScope.launch {
            val settings = PlanningSettings(
                availableHoursPerDay = state.availableHoursPerDay,
                unavailableWeekdays  = state.unavailableWeekdays,
                bufferDaysBeforeExam = state.bufferDaysBeforeExam,
                finalReviewDays      = state.finalReviewDays
            )
            val result = savePlanningSettingsUseCase(settings)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaved = true) }
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun onSavedConsumed() = _uiState.update { it.copy(isSaved = false) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    observePlanningSettingsUseCase = container.observePlanningSettingsUseCase,
                    savePlanningSettingsUseCase    = container.savePlanningSettingsUseCase
                )
            }
        }
    }
}
