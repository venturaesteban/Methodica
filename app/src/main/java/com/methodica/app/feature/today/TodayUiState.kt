package com.methodica.app.feature.today

import com.methodica.app.domain.model.Material
import com.methodica.app.domain.model.StudySession

data class TodaySessionItem(
    val session: StudySession,
    val subjectName: String,
    val assessmentTitle: String,
    val assessmentTypeLabel: String,
    val topicName: String?,
    val taskDescription: String,
    val resourceLinks: List<Material>
)

data class TodayUiState(
    val isLoading: Boolean = true,
    val sessions: List<TodaySessionItem> = emptyList()
)
