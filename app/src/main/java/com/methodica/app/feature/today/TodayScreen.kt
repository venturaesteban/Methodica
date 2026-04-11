package com.methodica.app.feature.today

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.methodica.app.domain.model.StudySessionStatus
import com.methodica.app.domain.model.StudySessionType

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    fun openResource(uriValue: String) {
        val uri = uriValue.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
            .onFailure {
                val message = if (it is ActivityNotFoundException) {
                    "No hay una app para abrir este recurso"
                } else {
                    "No se pudo abrir el recurso"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
    }

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
                itemsIndexed(uiState.sessions, key = { index, item -> "session-${item.session.id}-$index" }) { _, item ->
                    val isCompleted = item.session.status == StudySessionStatus.COMPLETED
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
                                val typeLabel = when (item.session.type) {
                                    StudySessionType.STUDY  -> "Estudio"
                                    StudySessionType.REVIEW -> "Repaso"
                                }
                                Text(typeLabel, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    item.subjectName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${item.assessmentTypeLabel}: ${item.assessmentTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                item.topicName?.let {
                                    Text(
                                        "Tema: $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    item.taskDescription,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${item.session.durationMinutes} min — sesión ${item.session.positionInDay + 1}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                val statusLabel = if (isCompleted) "Completada" else "Pendiente"
                                Text(statusLabel, style = MaterialTheme.typography.labelSmall)

                                if (item.resourceLinks.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Recursos",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    item.resourceLinks.forEach { material ->
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            FilledTonalButton(
                                                onClick = { openResource(material.uri) }
                                            ) {
                                                Text(material.title)
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }
                            }
                            if (!isCompleted) {
                                FilledTonalButton(onClick = { viewModel.onCompleteSession(item.session) }) {
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
