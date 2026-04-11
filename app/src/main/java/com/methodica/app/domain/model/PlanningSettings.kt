package com.methodica.app.domain.model

import java.time.DayOfWeek

data class PlanningSettings(
    val availableHoursPerDay:   Int = 4,
    val unavailableWeekdays:    Set<DayOfWeek> = emptySet(),
    val bufferDaysBeforeExam:   Int = 2,
    val finalReviewDays:        Int = 1,
    val remindersEnabled:       Boolean = true,
    val studentAge:             Int = 20,
    val studentCurrentDegreeIds: Set<Long> = emptySet(),
    val studentCompletedDegreeIds: Set<Long> = emptySet(),
    val readingComprehensionLevel: Int = 3,
    val passedSubjectIds:       Set<Long> = emptySet()
)
