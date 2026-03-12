package com.methodica.app.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.methodica.app.AppContainer
import com.methodica.app.domain.model.StudySessionStatus
import com.methodica.app.domain.model.StudySessionType

@Composable
fun TodayScreen(
    container: AppContainer,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Hoy", style = MaterialTheme.typography.headlineSmall)

        if (uiState.sessions.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No hay sesiones para hoy",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.sessions, key = { it.id }) { session ->
                    val isCompleted = session.status == StudySessionStatus.COMPLETED
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = if (isCompleted) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) else CardDefaults.cardColors()
                    ) {
                        Row(
                            modifier          = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val typeLabel = when (session.type) {
                                    StudySessionType.STUDY  -> "Estudio"
                                    StudySessionType.REVIEW -> "Repaso"
                                }
                                Text(typeLabel, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${session.durationMinutes} min — sesión ${session.positionInDay + 1}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                val statusLabel = if (isCompleted) "Completada" else "Pendiente"
                                Text(statusLabel, style = MaterialTheme.typography.labelSmall)
                            }
                            if (!isCompleted) {
                                FilledTonalButton(onClick = { viewModel.onCompleteSession(session) }) {
                                    Text("Completar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
