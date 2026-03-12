package com.methodica.app.domain.model

import java.time.DayOfWeek

data class PlanningSettings(
    val availableHoursPerDay:   Int = 4,
    val unavailableWeekdays:    Set<DayOfWeek> = emptySet(),
    val bufferDaysBeforeExam:   Int = 2,
    val finalReviewDays:        Int = 1
)
