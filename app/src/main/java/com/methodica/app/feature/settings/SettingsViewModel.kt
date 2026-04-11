package com.methodica.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methodica.app.data.work.ReminderScheduler
import com.methodica.app.domain.model.AiProviderPreset
import com.methodica.app.domain.model.AiProviderPresets
import com.methodica.app.domain.model.AiProviderSettings
import com.methodica.app.domain.model.PlanningSettings
import com.methodica.app.domain.usecase.academic.ObserveDegreesUseCase
import com.methodica.app.domain.usecase.subject.ObserveSubjectsUseCase
import com.methodica.app.domain.usecase.ai.ObserveAiProviderSettingsUseCase
import com.methodica.app.domain.usecase.ai.SaveAiProviderSettingsUseCase
import com.methodica.app.domain.usecase.planning.ObservePlanningSettingsUseCase
import com.methodica.app.domain.usecase.planning.SavePlanningSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observePlanningSettingsUseCase: ObservePlanningSettingsUseCase,
    private val savePlanningSettingsUseCase:    SavePlanningSettingsUseCase,
    private val observeDegreesUseCase:          ObserveDegreesUseCase,
    private val observeSubjectsUseCase:         ObserveSubjectsUseCase,
    private val observeAiProviderSettingsUseCase: ObserveAiProviderSettingsUseCase,
    private val saveAiProviderSettingsUseCase: SaveAiProviderSettingsUseCase,
    private val verifyAiConnectionUseCase:      com.methodica.app.domain.usecase.ai.VerifyAiConnectionUseCase,
    private val reminderScheduler:              ReminderScheduler
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
                        finalReviewDays      = settings.finalReviewDays,
                        remindersEnabled     = settings.remindersEnabled,
                        studentAge           = settings.studentAge,
                        studentCurrentDegreeIds = settings.studentCurrentDegreeIds,
                        studentCompletedDegreeIds = settings.studentCompletedDegreeIds,
                        readingComprehensionLevel = settings.readingComprehensionLevel,
                        passedSubjectIds     = settings.passedSubjectIds
                    )
                }
            }
        }

        viewModelScope.launch {
            observeDegreesUseCase().collect { degrees ->
                _uiState.update { state ->
                    val availableDegreeIds = degrees.map { it.id }.toSet()
                    val resolvedCurrent = state.studentCurrentDegreeIds.intersect(availableDegreeIds)
                    val resolvedCompleted = state.studentCompletedDegreeIds
                        .intersect(availableDegreeIds)
                        .minus(resolvedCurrent)
                    state.copy(
                        availableDegrees = degrees.sortedBy { it.name },
                        studentCurrentDegreeIds = resolvedCurrent,
                        studentCompletedDegreeIds = resolvedCompleted
                    )
                }
            }
        }

        viewModelScope.launch {
            observeSubjectsUseCase().collect { subjects ->
                _uiState.update { state ->
                    val allowedSubjectIds = if (state.studentCurrentDegreeIds.isEmpty()) {
                        subjects.map { it.id }.toSet()
                    } else {
                        subjects.filter { it.degreeId in state.studentCurrentDegreeIds }
                            .map { it.id }
                            .toSet()
                    }
                    val passedSubjectIds = state.passedSubjectIds.intersect(allowedSubjectIds)
                    state.copy(
                        availableSubjects = subjects,
                        passedSubjectIds = passedSubjectIds
                    )
                }
            }
        }

        viewModelScope.launch {
            observeAiProviderSettingsUseCase().collect { settings ->
                val preset = AiProviderPresets.providers.firstOrNull { provider ->
                    provider.displayName.equals(settings.providerName, ignoreCase = true) ||
                        provider.baseUrl.equals(settings.baseUrl, ignoreCase = true)
                }
                _uiState.update {
                    it.copy(
                        aiExternalEnabled = settings.externalEnabled,
                        aiProviderName = settings.providerName,
                        aiBaseUrl = settings.baseUrl,
                        aiModel = settings.model,
                        aiApiKey = settings.apiKey,
                        selectedProviderPreset = preset,
                        availableModels = preset?.suggestedModels.orEmpty(),
                        showProviderDropdown = false,
                        showModelDropdown = false
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

    fun onRemindersEnabledChange(enabled: Boolean) =
        _uiState.update { it.copy(remindersEnabled = enabled, error = null) }

    fun onStudentAgeChange(value: Int) =
        _uiState.update { it.copy(studentAge = value.coerceIn(10, 100), error = null) }

    fun onToggleCurrentDegree(degreeId: Long) {
        _uiState.update { state ->
            val updatedCurrent = if (degreeId in state.studentCurrentDegreeIds) {
                state.studentCurrentDegreeIds - degreeId
            } else {
                state.studentCurrentDegreeIds + degreeId
            }
            val updatedCompleted = state.studentCompletedDegreeIds - degreeId
            val allowedSubjectIds = if (updatedCurrent.isEmpty()) {
                state.availableSubjects.map { it.id }.toSet()
            } else {
                state.availableSubjects
                    .filter { it.degreeId in updatedCurrent }
                    .map { it.id }
                    .toSet()
            }
            state.copy(
                studentCurrentDegreeIds = updatedCurrent,
                studentCompletedDegreeIds = updatedCompleted,
                passedSubjectIds = state.passedSubjectIds.intersect(allowedSubjectIds),
                error = null
            )
        }
    }

    fun onToggleCompletedDegree(degreeId: Long) {
        _uiState.update { state ->
            val updatedCompleted = if (degreeId in state.studentCompletedDegreeIds) {
                state.studentCompletedDegreeIds - degreeId
            } else {
                state.studentCompletedDegreeIds + degreeId
            }
            state.copy(
                studentCompletedDegreeIds = updatedCompleted,
                studentCurrentDegreeIds = state.studentCurrentDegreeIds - degreeId,
                error = null
            )
        }
    }

    fun onReadingComprehensionLevelChange(value: Int) =
        _uiState.update { it.copy(readingComprehensionLevel = value.coerceIn(1, 5), error = null) }

    fun onTogglePassedSubject(subjectId: Long) {
        _uiState.update { state ->
            val updated = if (subjectId in state.passedSubjectIds) {
                state.passedSubjectIds - subjectId
            } else {
                state.passedSubjectIds + subjectId
            }
            state.copy(passedSubjectIds = updated, error = null)
        }
    }

    fun onAiProviderNameChange(value: String) =
        _uiState.update { it.copy(aiProviderName = value, error = null) }

    fun onAiExternalEnabledChange(enabled: Boolean) =
        _uiState.update { it.copy(aiExternalEnabled = enabled, error = null) }

    fun onAiBaseUrlChange(value: String) =
        _uiState.update { it.copy(aiBaseUrl = value, error = null) }

    fun onAiModelChange(value: String) =
        _uiState.update { it.copy(aiModel = value, error = null) }

    fun onAiApiKeyChange(value: String) =
        _uiState.update { it.copy(aiApiKey = value, error = null) }

    // 🆕 Métodos para manejo de proveedores preconfigurados
    fun onSelectAiProvider(preset: AiProviderPreset) {
        _uiState.update {
            it.copy(
                selectedProviderPreset = preset,
                aiProviderName = preset.displayName,
                aiBaseUrl = preset.baseUrl,
                availableModels = preset.suggestedModels,
                aiModel = preset.suggestedModels.firstOrNull() ?: "",
                showProviderDropdown = false,
                error = null
            )
        }
    }

    fun onSelectAiModel(model: String) {
        _uiState.update {
            it.copy(
                aiModel = model,
                showModelDropdown = false,
                error = null
            )
        }
    }

    fun onToggleProviderDropdown() {
        _uiState.update { it.copy(showProviderDropdown = !it.showProviderDropdown) }
    }

    fun onToggleModelDropdown() {
        _uiState.update { it.copy(showModelDropdown = !it.showModelDropdown) }
    }

    fun onSave() {
        val state = _uiState.value
        viewModelScope.launch {
            val settings = PlanningSettings(
                availableHoursPerDay = state.availableHoursPerDay,
                unavailableWeekdays  = state.unavailableWeekdays,
                bufferDaysBeforeExam = state.bufferDaysBeforeExam,
                finalReviewDays      = state.finalReviewDays,
                remindersEnabled     = state.remindersEnabled,
                studentAge           = state.studentAge,
                studentCurrentDegreeIds = state.studentCurrentDegreeIds,
                studentCompletedDegreeIds = state.studentCompletedDegreeIds,
                readingComprehensionLevel = state.readingComprehensionLevel,
                passedSubjectIds     = state.passedSubjectIds
            )
            val planningResult = savePlanningSettingsUseCase(settings)
            val aiResult = saveAiProviderSettingsUseCase(
                AiProviderSettings(
                    externalEnabled = state.aiExternalEnabled,
                    providerName = state.aiProviderName,
                    baseUrl = state.aiBaseUrl,
                    model = state.aiModel,
                    apiKey = state.aiApiKey
                )
            )

            if (planningResult.isSuccess && aiResult.isSuccess) {
                if (state.remindersEnabled) reminderScheduler.scheduleDaily()
                else reminderScheduler.cancelDaily()
                _uiState.update { it.copy(isSaved = true) }
            } else {
                _uiState.update {
                    it.copy(
                        error = planningResult.exceptionOrNull()?.message
                            ?: aiResult.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    fun onSavedConsumed() = _uiState.update { it.copy(isSaved = false) }

    fun onTestAiConnection() {
        val state = _uiState.value

        if (state.selectedProviderPreset == null) {
            _uiState.update {
                it.copy(
                    aiConnectionError = "Selecciona primero un proveedor",
                    aiConnectionResult = null
                )
            }
            return
        }

        if (state.aiBaseUrl.isBlank() || state.aiModel.isBlank() || state.aiApiKey.isBlank()) {
            _uiState.update { 
                it.copy(
                    aiConnectionError = "Proveedor, URL, modelo y API Key son requeridos",
                    aiConnectionResult = null
                )
            }
            return
        }

        _uiState.update { it.copy(isTestingAiConnection = true, aiConnectionError = null, aiConnectionResult = null) }

        viewModelScope.launch {
            val result = verifyAiConnectionUseCase(
                baseUrl = state.aiBaseUrl,
                model = state.aiModel,
                apiKey = state.aiApiKey
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isTestingAiConnection = false,
                        aiConnectionResult = "✓ Conexión exitosa. Proveedor IA respondió correctamente.",
                        aiConnectionError = null
                    )
                }
            } else {
                val errorMessage = result.exceptionOrNull()?.message 
                    ?: "Error desconocido al conectar con el proveedor"
                _uiState.update {
                    it.copy(
                        isTestingAiConnection = false,
                        aiConnectionError = errorMessage,
                        aiConnectionResult = null
                    )
                }
            }
        }
    }

    fun onConnectionResultConsumed() = _uiState.update { 
        it.copy(aiConnectionResult = null, aiConnectionError = null)
    }
}
