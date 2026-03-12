package com.methodica.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.methodica.app.AppContainer
import com.methodica.app.domain.model.StudySession
import com.methodica.app.domain.usecase.session.CompleteStudySessionUseCase
import com.methodica.app.domain.usecase.session.ObserveStudySessionsForDateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class TodayViewModel(
    private val observeSessionsForDateUseCase: ObserveStudySessionsForDateUseCase,
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
            observeSessionsForDateUseCase(todayMillis).collect { sessions ->
                _uiState.update { it.copy(isLoading = false, sessions = sessions) }
            }
        }
    }

    fun onCompleteSession(session: StudySession) {
        viewModelScope.launch {
            completeStudySessionUseCase(session)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TodayViewModel(
                    observeSessionsForDateUseCase = container.observeStudySessionsForDateUseCase,
                    completeStudySessionUseCase   = container.completeStudySessionUseCase
                )
            }
        }
    }
}
