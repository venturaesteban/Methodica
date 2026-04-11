package com.methodica.app.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TopicFormScreen(
    topicId:        Long?,
    onNavigateBack: () -> Unit
) {
    val viewModel: TopicFormViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value          = uiState.name,
            onValueChange  = viewModel::onNameChange,
            label          = { Text("Nombre del tema *") },
            isError        = uiState.nameError != null,
            supportingText = uiState.nameError?.let { { Text(it) } },
            modifier       = Modifier.fillMaxWidth(),
            singleLine     = true
        )

        Text("Dificultad", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..3).forEach { level ->
                FilterChip(
                    selected = uiState.difficulty == level,
                    onClick  = { viewModel.onDifficultyChange(level) },
                    label    = { Text("${"★".repeat(level)}") }
                )
            }
        }

        // Horas estimadas como campo numérico
        OutlinedTextField(
            value          = uiState.estimatedHours.toString(),
            onValueChange  = { v -> v.toIntOrNull()?.let { viewModel.onEstimatedHoursChange(it) } },
            label          = { Text("Horas estimadas *") },
            isError        = uiState.estimatedHoursError != null,
            supportingText = uiState.estimatedHoursError?.let { { Text(it) } },
            modifier       = Modifier.fillMaxWidth(),
            singleLine     = true
        )

        OutlinedTextField(
            value         = uiState.order.toString(),
            onValueChange = { v -> v.toIntOrNull()?.let { viewModel.onOrderChange(it) } },
            label         = { Text("Orden") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true
        )

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick  = viewModel::onSave,
            enabled  = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (topicId != null) "Guardar cambios" else "Crear tema")
        }

        TextButton(
            onClick  = onNavigateBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Cancelar")
        }
    }
}
