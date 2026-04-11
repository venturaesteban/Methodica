package com.methodica.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methodica.app.domain.model.Material
import com.methodica.app.domain.model.StudySession
import com.methodica.app.domain.usecase.session.CompleteStudySessionUseCase
import com.methodica.app.domain.usecase.assessment.GetAssessmentUseCase
import com.methodica.app.domain.usecase.material.ObserveAllMaterialsUseCase
import com.methodica.app.domain.usecase.session.ObserveStudySessionsForDateUseCase
import com.methodica.app.domain.usecase.subject.GetSubjectUseCase
import com.methodica.app.domain.usecase.topic.GetTopicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val observeSessionsForDateUseCase: ObserveStudySessionsForDateUseCase,
    private val observeAllMaterialsUseCase:   ObserveAllMaterialsUseCase,
    private val getSubjectUseCase:            GetSubjectUseCase,
    private val getAssessmentUseCase:         GetAssessmentUseCase,
    private val getTopicUseCase:              GetTopicUseCase,
    private val completeStudySessionUseCase:   CompleteStudySessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        val todayMillis = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        viewModelScope.launch {
            combine(
                observeSessionsForDateUseCase(todayMillis),
                observeAllMaterialsUseCase()
            ) { sessions, materials ->
                buildTodayItems(sessions, materials)
            }.collect { enrichedSessions ->
                _uiState.update { it.copy(isLoading = false, sessions = enrichedSessions) }
            }
        }
    }

    fun onCompleteSession(session: StudySession) {
        viewModelScope.launch {
            completeStudySessionUseCase(session)
        }
    }

    private suspend fun buildTodayItems(
        sessions: List<StudySession>,
        materials: List<Material>
    ): List<TodaySessionItem> {
        return sessions.map { session ->
            val subject = getSubjectUseCase(session.subjectId)
            val assessment = getAssessmentUseCase(session.assessmentId)
            val topic = session.topicId?.let { getTopicUseCase(it) }

            val taskDescription = assessment?.notes
                ?.takeIf { it.isNotBlank() }
                ?: topic?.let { "Repasar y practicar ${it.name}" }
                ?: "Preparar ${assessment?.title ?: "la evaluación"}"

            TodaySessionItem(
                session = session,
                subjectName = subject?.name ?: "Asignatura",
                assessmentTitle = assessment?.title ?: "Evaluación",
                assessmentTypeLabel = assessment?.type?.displayName ?: "Evaluación",
                topicName = topic?.name,
                taskDescription = taskDescription,
                resourceLinks = materials
                    .filter { material ->
                        material.subjectId == session.subjectId &&
                            (session.topicId == null || material.topicId == null || material.topicId == session.topicId)
                    }
                    .sortedByDescending { it.topicId == session.topicId }
                    .take(3)
            )
        }
    }
}
