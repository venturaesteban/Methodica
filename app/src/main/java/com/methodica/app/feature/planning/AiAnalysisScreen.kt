package com.methodica.app.feature.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.methodica.app.core.ai.AiDocumentProcessingPolicy
import com.methodica.app.domain.model.AiExecutionMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAnalysisScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var assessmentDropdownOpen by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
        Text("Análisis IA del temario", style = MaterialTheme.typography.headlineSmall)

        ExposedDropdownMenuBox(
            expanded = assessmentDropdownOpen,
            onExpandedChange = { assessmentDropdownOpen = it }
        ) {
            OutlinedTextField(
                value = uiState.selectedAssessment?.title ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Evaluación") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assessmentDropdownOpen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = assessmentDropdownOpen,
                onDismissRequest = { assessmentDropdownOpen = false }
            ) {
                uiState.assessments.forEach { assessment ->
                    DropdownMenuItem(
                        text = { Text(assessment.title) },
                        onClick = {
                            viewModel.onSelectAssessment(assessment.id)
                            assessmentDropdownOpen = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = uiState.sourceText,
            onValueChange = viewModel::onSourceTextChange,
            label = { Text("Texto del documento/temario") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = uiState.aiExecutionMode == AiExecutionMode.HEURISTIC,
                onClick = { viewModel.onAiExecutionModeChange(AiExecutionMode.HEURISTIC) },
                label = { Text("Heurístico") }
            )
            FilterChip(
                selected = uiState.aiExecutionMode == AiExecutionMode.EXTERNAL,
                onClick = { viewModel.onAiExecutionModeChange(AiExecutionMode.EXTERNAL) },
                enabled = uiState.canUseExternalAi,
                label = { Text("IA configurada") }
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = when {
                    uiState.localModelsReady ->
                        "Gemma 3n local lista: Methodica prioriza reasoning local con retrieval por embeddings en este dispositivo."
                    !uiState.runtimeMessage.isNullOrBlank() && uiState.aiExecutionMode == AiExecutionMode.EXTERNAL ->
                        "${uiState.runtimeMessage} Si mantienes IA configurada, ese proveedor solo se usara cuando el runtime local no pueda responder."
                    !uiState.runtimeMessage.isNullOrBlank() ->
                        "${uiState.runtimeMessage} Se usara fallback heuristico mientras Gemma 3n no este operativa."
                    else ->
                        "Gemma 3n local aun no esta operativa. Se usara fallback heuristico hasta que el runtime quede listo."
                },
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (uiState.aiExecutionMode == AiExecutionMode.EXTERNAL) {
            Text(
                "Usar IA configurada consumirá créditos de tu suscripción.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(
            text = "Para una evaluacion aproximada de temario, la app procesa como maximo ${AiDocumentProcessingPolicy.MAX_PDF_PAGES_TEXT} paginas por documento (si requiere OCR: ${AiDocumentProcessingPolicy.MAX_PDF_PAGES_OCR} paginas).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = viewModel::onAnalyze,
            enabled = !uiState.isAnalyzing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isAnalyzing) "Analizando…" else "Analizar")
        }

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        uiState.infoMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        if (uiState.analysisId != null) {
            OutlinedTextField(
                value = uiState.estimatedScope,
                onValueChange = viewModel::onEstimatedScopeChange,
                label = { Text("Alcance estimado (editable)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )

            OutlinedTextField(
                value = uiState.justification,
                onValueChange = viewModel::onJustificationChange,
                label = { Text("Justificación (editable)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Confianza ${(uiState.confidence * 100).toInt()}%")
                if (uiState.requiresConfirmation) {
                    FilterChip(selected = true, onClick = {}, label = { Text("Requiere confirmación") })
                }
            }

            Text("Temas detectados", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.topicEdits, key = { it.topicName }) { topic ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(topic.topicName, style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Incluido en alcance")
                                Switch(
                                    checked = topic.isIncludedInScope,
                                    onCheckedChange = { viewModel.onToggleIncluded(topic.topicName) }
                                )
                            }
                            OutlinedTextField(
                                value = topic.complexityLevel.toString(),
                                onValueChange = { value -> value.toIntOrNull()?.let { viewModel.onComplexityChange(topic.topicName, it) } },
                                label = { Text("Complejidad (1-5)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = topic.recommendedHours.toString(),
                                onValueChange = { value -> value.toIntOrNull()?.let { viewModel.onHoursChange(topic.topicName, it) } },
                                label = { Text("Horas recomendadas") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Text(topic.rationale, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }

            Button(
                onClick = viewModel::onSaveEdits,
                enabled = !uiState.isSavingEdits,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSavingEdits) "Guardando…" else "Guardar edición")
            }

            Button(
                onClick = viewModel::onApplyAndRegenerate,
                enabled = !uiState.isApplyingAndRegenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isApplyingAndRegenerating) "Aplicando…" else "Aplicar y regenerar plan")
            }
        }

        Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
            Text("Volver")
        }
    }
}
