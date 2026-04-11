package com.methodica.app.feature.home

import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.Degree

enum class HomeAlertType {
	NO_SESSION_TODAY,
	REPLAN_REQUIRED,
	UPCOMING_DEADLINE
}

data class HomeAlert(
	val type: HomeAlertType,
	val message: String
)

data class HomeTodaySummary(
	val title: String,
	val details: String,
	val hours: Float,
	val totalItems: Int
)

data class HomeCalendarDay(
	val dateEpochMillis: Long,
	val dayOfMonth: Int,
	val eventsCount: Int
)

data class HomeUiState(
	val isLoading: Boolean = true,
	val totalSubjects: Int = 0,
	val sessionsTodayCount: Int = 0,
	val upcomingSessionsCount: Int = 0,
	val activeDegrees: List<Degree> = emptyList(),
	val upcomingAssessments: List<Assessment> = emptyList(),
	val alerts: List<HomeAlert> = emptyList(),
	val todaySummary: HomeTodaySummary? = null,
	val calendarMonthLabel: String = "",
	val calendarDays: List<HomeCalendarDay> = emptyList(),
	val error: String? = null
)
