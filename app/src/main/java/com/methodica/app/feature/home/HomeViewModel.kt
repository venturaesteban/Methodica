package com.methodica.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methodica.app.domain.model.DegreeStatus
import com.methodica.app.domain.model.StudySession
import com.methodica.app.domain.model.StudySessionStatus
import com.methodica.app.domain.model.StudySessionType
import com.methodica.app.domain.usecase.academic.ObserveDegreesUseCase
import com.methodica.app.domain.usecase.assessment.ObserveAllAssessmentsUseCase
import com.methodica.app.domain.usecase.session.ObserveStudySessionsForDateUseCase
import com.methodica.app.domain.usecase.session.ObserveStudySessionsForRangeUseCase
import com.methodica.app.domain.usecase.subject.ObserveSubjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeSubjectsUseCase: ObserveSubjectsUseCase,
    observeDegreesUseCase: ObserveDegreesUseCase,
    observeAllAssessmentsUseCase: ObserveAllAssessmentsUseCase,
    observeStudySessionsForDateUseCase: ObserveStudySessionsForDateUseCase,
    observeStudySessionsForRangeUseCase: ObserveStudySessionsForRangeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val today = LocalDate.now()
        val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val weekEndMillis = today.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dangerLimitMillis = today.plusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val previousWindowStartMillis = today.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val yesterdayMillis = today.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val currentMonth = YearMonth.from(today)
        val monthStart = currentMonth.atDay(1)
        val monthEnd = currentMonth.atEndOfMonth()
        val monthStartMillis = monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val monthEndMillis = monthEnd.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            val academicFlow = combine(
                observeSubjectsUseCase(),
                observeDegreesUseCase(),
                observeAllAssessmentsUseCase()
            ) { subjects, degrees, assessments ->
                Triple(subjects, degrees, assessments)
            }

            val sessionsFlow = combine(
                observeStudySessionsForDateUseCase(todayMillis),
                observeStudySessionsForRangeUseCase(todayMillis, weekEndMillis),
                observeStudySessionsForRangeUseCase(previousWindowStartMillis, yesterdayMillis),
                observeStudySessionsForRangeUseCase(monthStartMillis, monthEndMillis)
            ) { sessionsToday, sessionsWeek, previousSessions, monthSessions ->
                HomeSessionBuckets(
                    sessionsToday = sessionsToday,
                    sessionsWeek = sessionsWeek,
                    previousSessions = previousSessions,
                    monthSessions = monthSessions
                )
            }

            combine(academicFlow, sessionsFlow) { academic, sessionBuckets ->
                val (subjects, degrees, assessments) = academic
                val sessionsToday = sessionBuckets.sessionsToday
                val sessionsWeek = sessionBuckets.sessionsWeek
                val previousSessions = sessionBuckets.previousSessions
                val monthSessions = sessionBuckets.monthSessions

                val activeDegrees = degrees.filter { it.status == DegreeStatus.ACTIVE }
                val upcomingAssessments = assessments
                    .filter { it.date in todayMillis..weekEndMillis }
                    .sortedBy { it.date }

                val monthAssessments = assessments
                    .filter { it.date in monthStartMillis..monthEndMillis }

                val sessionsByAssessment = sessionsWeek.groupBy { it.assessmentId }
                val riskyAssessments = upcomingAssessments.filter {
                    it.date <= dangerLimitMillis && (sessionsByAssessment[it.id]?.isEmpty() != false)
                }

                val pendingPreviousSessions = previousSessions.count { it.status != StudySessionStatus.COMPLETED }

                val alertItems = buildList {
                    if (sessionsToday.isEmpty()) {
                        add(
                            HomeAlert(
                                type = HomeAlertType.NO_SESSION_TODAY,
                                message = "No tienes sesiones planificadas para hoy"
                            )
                        )
                    }
                    if (pendingPreviousSessions > 0) {
                        add(
                            HomeAlert(
                                type = HomeAlertType.REPLAN_REQUIRED,
                                message = "Tienes $pendingPreviousSessions sesiones pendientes de días anteriores. Replanifica."
                            )
                        )
                    }
                    if (riskyAssessments.isNotEmpty()) {
                        add(
                            HomeAlert(
                                type = HomeAlertType.UPCOMING_DEADLINE,
                                message = "${riskyAssessments.size} evaluaciones próximas sin sesiones suficientes"
                            )
                        )
                    }
                }

                val subjectsById = subjects.associateBy { it.id }
                val assessmentsById = assessments.associateBy { it.id }
                val firstTodaySession = sessionsToday.sortedBy { it.positionInDay }.firstOrNull()
                val todaySummary = firstTodaySession?.let { session ->
                    val subjectName = subjectsById[session.subjectId]?.name ?: "Materia"
                    val assessmentTitle = assessmentsById[session.assessmentId]?.title ?: "Sesión"
                    val typeLabel = when (session.type) {
                        StudySessionType.STUDY -> "Estudio"
                        StudySessionType.REVIEW -> "Repaso"
                    }
                    HomeTodaySummary(
                        title = assessmentTitle,
                        details = "$typeLabel • $subjectName",
                        hours = session.durationMinutes / 60f,
                        totalItems = sessionsToday.size
                    )
                }

                val sessionCountByDate = monthSessions.groupBy { it.date }.mapValues { it.value.size }
                val assessmentCountByDate = monthAssessments.groupBy { it.date }.mapValues { it.value.size }
                val calendarDays = (1..currentMonth.lengthOfMonth()).map { day ->
                    val date = currentMonth.atDay(day)
                    val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    HomeCalendarDay(
                        dateEpochMillis = dateMillis,
                        dayOfMonth = day,
                        eventsCount = (sessionCountByDate[dateMillis] ?: 0) + (assessmentCountByDate[dateMillis] ?: 0)
                    )
                }

                HomeUiState(
                    isLoading = false,
                    totalSubjects = subjects.size,
                    sessionsTodayCount = sessionsToday.size,
                    upcomingSessionsCount = sessionsWeek.size,
                    activeDegrees = activeDegrees,
                    upcomingAssessments = upcomingAssessments,
                    alerts = alertItems,
                    todaySummary = todaySummary,
                    calendarMonthLabel = "${today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${today.year}",
                    calendarDays = calendarDays
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private data class HomeSessionBuckets(
        val sessionsToday: List<StudySession>,
        val sessionsWeek: List<StudySession>,
        val previousSessions: List<StudySession>,
        val monthSessions: List<StudySession>
    )
}
