package com.methodica.app.feature.planning

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.methodica.app.domain.model.AiExecutionMode
import com.methodica.app.domain.model.PlanningStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    onNavigateToAiAnalysis: (Long) -> Unit,
    viewModel: PlanningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Planificación", style = MaterialTheme.typography.headlineSmall)

        // Selector de evaluación
        var dropdownOpen by remember { mutableStateOf(false) }
        val subjectMap = uiState.subjects.associateBy { it.id }

        ExposedDropdownMenuBox(
            expanded         = dropdownOpen,
            onExpandedChange = { dropdownOpen = it }
        ) {
            OutlinedTextField(
                value         = uiState.selectedAssessment?.let { a ->
                    val subjectName = subjectMap[a.subjectId]?.name ?: ""
                    "$subjectName — ${a.title}"
                } ?: "",
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Seleccionar evaluación") },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownOpen) },
                modifier      = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded         = dropdownOpen,
                onDismissRequest = { dropdownOpen = false }
            ) {
                uiState.assessments.forEach { assessment ->
                    val subjectName = subjectMap[assessment.subjectId]?.name ?: ""
                    DropdownMenuItem(
                        text = { Text("$subjectName — ${assessment.title} (${dateFormatter.format(Date(assessment.date))})") },
                        onClick = {
                            viewModel.onSelectAssessment(assessment)
                            dropdownOpen = false
                        }
                    )
                }
                if (uiState.assessments.isEmpty()) {
                    DropdownMenuItem(
                        text    = { Text("No hay evaluaciones creadas") },
                        onClick = { dropdownOpen = false },
                        enabled = false
                    )
                }
            }
        }

        // Resumen de topics asociados
        if (uiState.selectedAssessment != null) {
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = when {
                        uiState.localModelsReady ->
                            "Gemma 3n local lista: este flujo prioriza reasoning local con evidencia indexada en el dispositivo."
                        !uiState.runtimeMessage.isNullOrBlank() && uiState.aiExecutionMode == AiExecutionMode.EXTERNAL ->
                            "${uiState.runtimeMessage} Si mantienes IA configurada, ese proveedor solo entrara cuando el runtime local no responda."
                        !uiState.runtimeMessage.isNullOrBlank() ->
                            "${uiState.runtimeMessage} Se usara fallback heuristico hasta que Gemma 3n quede lista."
                        else ->
                            "Gemma 3n local aun no esta operativa. Methodica seguira en flujo degradado."
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Temas asociados (${uiState.linkedTopics.size})", style = MaterialTheme.typography.titleSmall)
                    if (uiState.linkedTopics.isEmpty()) {
                        Text(
                            "Sin temas. Asocia temas desde la edición de la evaluación.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        uiState.linkedTopics.forEach { topic ->
                            Text("• ${topic.name} — ${topic.estimatedHours}h (dif. ${topic.difficulty})",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = uiState.aiInputText,
                onValueChange = viewModel::onAiInputChange,
                label = { Text("Texto del temario para análisis IA") },
                placeholder = { Text("Pega aquí guía docente, temario o instrucciones del profesor") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8
            )

            Button(
                onClick = viewModel::onAnalyzeWithAi,
                enabled = !uiState.isAnalyzingAi,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isAnalyzingAi) "Analizando…" else "Analizar con IA")
            }

            Button(
                onClick = { onNavigateToAiAnalysis(uiState.selectedAssessment!!.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abrir análisis IA avanzado")
            }

            uiState.aiError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            uiState.lastAiInsight?.let { insight ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Análisis IA", style = MaterialTheme.typography.titleSmall)
                        Text(insight.summary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Confianza: ${(insight.confidence * 100).toInt()}%${if (insight.requiresConfirmation) " • requiere confirmación" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (insight.requiresConfirmation) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Alcance estimado: ${insight.scopeInference.estimatedScope.take(180)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        insight.recommendation.warnings.forEach { warning ->
                            Text("⚠ $warning", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Button(
                onClick  = viewModel::onGenerate,
                enabled  = !uiState.isGenerating && uiState.linkedTopics.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isGenerating) "Generando…" else "Generar plan")
            }
        }

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        // Resultado
        uiState.lastResult?.let { result ->
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Resultado", style = MaterialTheme.typography.titleMedium)
                        val (label, color) = when (result.status) {
                            PlanningStatus.FEASIBLE   -> "FACTIBLE" to MaterialTheme.colorScheme.primary
                            PlanningStatus.TIGHT      -> "AJUSTADO" to MaterialTheme.colorScheme.tertiary
                            PlanningStatus.INFEASIBLE -> "INVIABLE" to MaterialTheme.colorScheme.error
                        }
                        Text(label, color = color, style = MaterialTheme.typography.labelLarge)
                    }

                    Text("Horas de estudio: ${result.totalStudyHours}", style = MaterialTheme.typography.bodyMedium)
                    Text("Horas de repaso: ${result.totalReviewHours}", style = MaterialTheme.typography.bodyMedium)
                    Text("Sesiones creadas: ${result.sessions.size}", style = MaterialTheme.typography.bodyMedium)

                    if (result.unscheduledHours > 0) {
                        Text(
                            "Horas sin asignar: ${result.unscheduledHours}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    result.warnings.forEach { w ->
                        Text("⚠ $w", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
