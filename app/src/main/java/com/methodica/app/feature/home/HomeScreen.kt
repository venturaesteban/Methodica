package com.methodica.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToToday: () -> Unit = {},
    onNavigateToPlanning: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            Text("Resumen de hoy", style = MaterialTheme.typography.titleMedium)
        }

        item {
            val summary = uiState.todaySummary
            Card(modifier = Modifier.fillMaxWidth()) {
                if (summary == null) {
                    Text(
                        text = "No hay sesión principal para hoy",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(summary.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(summary.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${summary.hours} h • ${summary.totalItems} sesiones hoy", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Text("Calendario", style = MaterialTheme.typography.titleMedium)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(uiState.calendarMonthLabel, style = MaterialTheme.typography.titleSmall)
                    uiState.calendarDays.chunked(7).forEach { weekDays ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            weekDays.forEach { day ->
                                Card(modifier = Modifier.weight(1f)) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            if (day.eventsCount > 0) "•" else "",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                            repeat(7 - weekDays.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Titulaciones activas", style = MaterialTheme.typography.titleMedium)
        }

        if (uiState.activeDegrees.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No hay titulaciones activas",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(uiState.activeDegrees, key = { index, degree -> "degree-${degree.id}-$index" }) { _, degree ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(degree.name, style = MaterialTheme.typography.titleSmall)
                        if (!degree.description.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                degree.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (uiState.alerts.isNotEmpty()) {
            item {
                Text("Alertas", style = MaterialTheme.typography.titleMedium)
            }
            items(uiState.alerts) { alert ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "⚠ ${alert.message}",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        item {
            Text("Próximas evaluaciones", style = MaterialTheme.typography.titleMedium)
        }

        if (uiState.upcomingAssessments.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Sin evaluaciones en los próximos 7 días",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(uiState.upcomingAssessments, key = { index, assessment -> "assessment-${assessment.id}-$index" }) { _, assessment ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(assessment.title, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${assessment.type.displayName} • ${dateFormat.format(Date(assessment.date))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
