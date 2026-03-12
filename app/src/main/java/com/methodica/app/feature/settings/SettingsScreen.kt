package com.methodica.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.methodica.app.AppContainer
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbar.showSnackbar("Ajustes guardados")
            viewModel.onSavedConsumed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Ajustes de planificación", style = MaterialTheme.typography.headlineSmall)

        // Horas disponibles por día
        Column {
            Text("Horas de estudio por día: ${uiState.availableHoursPerDay}", style = MaterialTheme.typography.titleSmall)
            Slider(
                value         = uiState.availableHoursPerDay.toFloat(),
                onValueChange = { viewModel.onHoursPerDayChange(it.roundToInt()) },
                valueRange    = 1f..16f,
                steps         = 14,
                modifier      = Modifier.fillMaxWidth()
            )
        }

        // Días no disponibles
        Column {
            Text("Días no disponibles", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = day in uiState.unavailableWeekdays,
                        onClick  = { viewModel.onToggleWeekday(day) },
                        label    = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) }
                    )
                }
            }
        }

        // Colchón antes del examen
        Column {
            Text("Colchón antes del examen: ${uiState.bufferDaysBeforeExam} días", style = MaterialTheme.typography.titleSmall)
            Slider(
                value         = uiState.bufferDaysBeforeExam.toFloat(),
                onValueChange = { viewModel.onBufferDaysChange(it.roundToInt()) },
                valueRange    = 0f..14f,
                steps         = 13,
                modifier      = Modifier.fillMaxWidth()
            )
        }

        // Días de repaso final
        Column {
            Text("Días de repaso final: ${uiState.finalReviewDays}", style = MaterialTheme.typography.titleSmall)
            Slider(
                value         = uiState.finalReviewDays.toFloat(),
                onValueChange = { viewModel.onFinalReviewDaysChange(it.roundToInt()) },
                valueRange    = 0f..7f,
                steps         = 6,
                modifier      = Modifier.fillMaxWidth()
            )
        }

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = viewModel::onSave) {
                Text("Guardar")
            }
        }

        SnackbarHost(hostState = snackbar)
    }
}
