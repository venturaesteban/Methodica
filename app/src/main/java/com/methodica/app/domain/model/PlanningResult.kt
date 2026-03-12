package com.methodica.app.domain.model

data class PlanningResult(
    val sessions:         List<StudySession>,
    val status:           PlanningStatus,
    val totalStudyHours:  Int,
    val totalReviewHours: Int,
    val unscheduledHours: Int,
    val warnings:         List<String> = emptyList()
)
