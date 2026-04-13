package com.methodica.app.feature.settings

import com.methodica.app.domain.model.AiProviderPreset
import com.methodica.app.domain.model.AiProviderPresets
import com.methodica.app.domain.model.Degree
import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.ai.local.LocalModelInstallState
import java.time.DayOfWeek

data class SettingsUiState(
    val isLoading:            Boolean = true,
    val availableHoursPerDay: Int = 4,
    val unavailableWeekdays:  Set<DayOfWeek> = emptySet(),
    val bufferDaysBeforeExam: Int = 2,
    val finalReviewDays:      Int = 1,
    val remindersEnabled:     Boolean = true,
    val studentAge:           Int = 20,
    val studentCurrentDegreeIds: Set<Long> = emptySet(),
    val studentCompletedDegreeIds: Set<Long> = emptySet(),
    val readingComprehensionLevel: Int = 3,
    val passedSubjectIds:     Set<Long> = emptySet(),
    val availableDegrees:     List<Degree> = emptyList(),
    val availableSubjects:    List<Subject> = emptyList(),
    val aiExternalEnabled:    Boolean = false,
    val aiProviderName:       String = "",
    val aiBaseUrl:            String = "",
    val aiModel:              String = "",
    val aiApiKey:             String = "",
    val isSaved:              Boolean = false,
    val error:                String? = null,
    val isTestingAiConnection: Boolean = false,
    val aiConnectionResult:   String? = null,
    val aiConnectionError:    String? = null,
    // 🆕 Propiedades para selección de proveedor
    val availableProviders:   List<AiProviderPreset> = AiProviderPresets.providers,
    val selectedProviderPreset: AiProviderPreset? = null,
    val availableModels:      List<String> = emptyList(),
    val showProviderDropdown: Boolean = false,
    val showModelDropdown:    Boolean = false,
    val localModelStates:     List<LocalModelInstallState> = emptyList()
) {
    val availablePassedSubjects: List<Subject>
        get() = availableSubjects
            .asSequence()
            .filter { subject ->
                studentCurrentDegreeIds.isEmpty() || subject.degreeId in studentCurrentDegreeIds
            }
            .toList()
            .sortedWith(compareBy<Subject> { it.courseYear }.thenBy { it.name })
}
