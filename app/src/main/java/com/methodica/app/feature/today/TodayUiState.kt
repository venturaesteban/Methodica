package com.methodica.app.feature.today

import com.methodica.app.domain.model.StudySession

data class TodayUiState(
    val isLoading: Boolean = true,
    val sessions:  List<StudySession> = emptyList()
)
