package com.methodica.app.feature.settings

import java.time.DayOfWeek

data class SettingsUiState(
    val isLoading:            Boolean = true,
    val availableHoursPerDay: Int = 4,
    val unavailableWeekdays:  Set<DayOfWeek> = emptySet(),
    val bufferDaysBeforeExam: Int = 2,
    val finalReviewDays:      Int = 1,
    val isSaved:              Boolean = false,
    val error:                String? = null
)
