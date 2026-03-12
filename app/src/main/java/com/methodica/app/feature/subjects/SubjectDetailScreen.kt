package com.methodica.app.feature.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.methodica.app.AppContainer
import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.Topic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubjectDetailScreen(
    subjectId:                Long,
    container:                AppContainer,
    onNavigateBack:           () -> Unit,
    onNavigateToEditForm:     () -> Unit,
    onNavigateToTopicForm:    (Long?) -> Unit,
    onNavigateToAssessmentForm: (Long?) -> Unit
) {
    val viewModel: SubjectDetailViewModel = viewModel(
        key     = "subjectDetail_$subjectId",
        factory = SubjectDetailViewModel.factory(subjectId, container)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var confirmDeleteSubject by remember { mutableStateOf(false) }
    var topicPendingDelete   by remember { mutableStateOf<Topic?>(null) }
    var assessmentPendingDelete by remember { mutableStateOf<Assessment?>(null) }

    // Redirigir atrás si la materia fue eliminada
    LaunchedEffect(uiState.subject, uiState.isLoading) {
        if (!uiState.isLoading && uiState.subject == null) onNavigateBack()
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val subject = uiState.subject ?: return

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToAssessmentForm(null) },
                    icon    = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text    = { Text("Evaluación") }
                )
                Spacer(Modifier.height(8.dp))
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToTopicForm(null) },
                    icon    = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text    = { Text("Tema") }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header de la materia
            item {
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(16.dp).clip(CircleShape)
                                    .background(parseSubjectColor(subject.colorHex))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(subject.name, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = onNavigateToEditForm) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = { confirmDeleteSubject = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                            }
                        }
                        if (!subject.description.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text  = subject.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Sección de temas
            item {
                Row(
                    modifier            = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    Text("Temas", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
            }
            if (uiState.topics.isEmpty()) {
                item { Text("Sin temas aún", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(uiState.topics, key = { "topic_${it.id}" }) { topic ->
                    TopicItem(
                        topic    = topic,
                        onEdit   = { onNavigateToTopicForm(topic.id) },
                        onDelete = { topicPendingDelete = topic }
                    )
                }
            }

            // Sección de evaluaciones
            item {
                Row(
                    modifier            = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    Text("Evaluaciones", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
            }
            if (uiState.assessments.isEmpty()) {
                item { Text("Sin evaluaciones aún", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(uiState.assessments, key = { "assessment_${it.id}" }) { assessment ->
                    AssessmentItem(
                        assessment = assessment,
                        onEdit     = { onNavigateToAssessmentForm(assessment.id) },
                        onDelete   = { assessmentPendingDelete = assessment }
                    )
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    // Confirmar borrar materia
    if (confirmDeleteSubject) {
        AlertDialog(
            onDismissRequest = { confirmDeleteSubject = false },
            title   = { Text("Eliminar materia") },
            text    = { Text("¿Eliminar \"${subject.name}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteSubject(onNavigateBack)
                    confirmDeleteSubject = false
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSubject = false }) { Text("Cancelar") }
            }
        )
    }

    topicPendingDelete?.let { topic ->
        AlertDialog(
            onDismissRequest = { topicPendingDelete = null },
            title   = { Text("Eliminar tema") },
            text    = { Text("¿Eliminar \"${topic.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteTopic(topic)
                    topicPendingDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { topicPendingDelete = null }) { Text("Cancelar") }
            }
        )
    }

    assessmentPendingDelete?.let { assessment ->
        AlertDialog(
            onDismissRequest = { assessmentPendingDelete = null },
            title   = { Text("Eliminar evaluación") },
            text    = { Text("¿Eliminar \"${assessment.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteAssessment(assessment)
                    assessmentPendingDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { assessmentPendingDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TopicItem(topic: Topic, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(topic.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text  = "Dificultad: ${"★".repeat(topic.difficulty)}${"☆".repeat(3 - topic.difficulty)}  •  ${topic.estimatedHours}h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Filled.Edit,   contentDescription = "Editar") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
        }
    }
}

@Composable
private fun AssessmentItem(assessment: Assessment, onEdit: () -> Unit, onDelete: () -> Unit) {
    val dateText = remember(assessment.date) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(assessment.date))
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(assessment.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text  = "${assessment.type.displayName}  •  $dateText${assessment.weight?.let { "  •  $it%" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Filled.Edit,   contentDescription = "Editar") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
        }
    }
}

private fun parseSubjectColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color.Gray)
