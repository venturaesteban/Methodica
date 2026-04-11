package com.methodica.app.feature.materials

import android.content.Intent
import android.net.Uri
import com.methodica.app.core.ai.AiDocumentProcessingPolicy
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.methodica.app.domain.model.MaterialType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MaterialFormScreen(
    materialId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: MaterialFormViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val mimeType = context.contentResolver.getType(it)
            viewModel.onPickedLocalFile(it.toString(), mimeType)
        }
    }

    var subjectDropdownOpen by remember { mutableStateOf(false) }
    var degreeDropdownOpen by remember { mutableStateOf(false) }
    var courseDropdownOpen by remember { mutableStateOf(false) }
    var topicDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (materialId == null) "Nuevo material" else "Editar material",
            style = MaterialTheme.typography.headlineSmall
        )

        ExposedDropdownMenuBox(
            expanded = degreeDropdownOpen,
            onExpandedChange = { degreeDropdownOpen = it }
        ) {
            OutlinedTextField(
                value = uiState.selectedDegreeId?.let { id ->
                    uiState.availableDegrees.firstOrNull { it.first == id }?.second
                } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Titulacion") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = degreeDropdownOpen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = degreeDropdownOpen,
                onDismissRequest = { degreeDropdownOpen = false }
            ) {
                uiState.availableDegrees.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            viewModel.onSelectDegree(id)
                            degreeDropdownOpen = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = courseDropdownOpen,
            onExpandedChange = { courseDropdownOpen = it }
        ) {
            OutlinedTextField(
                value = uiState.selectedCourseYear?.let { "Curso $it" } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Curso") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownOpen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = courseDropdownOpen,
                onDismissRequest = { courseDropdownOpen = false }
            ) {
                uiState.availableCourseYears.forEach { year ->
                    DropdownMenuItem(
                        text = { Text("Curso $year") },
                        onClick = {
                            viewModel.onSelectCourseYear(year)
                            courseDropdownOpen = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = subjectDropdownOpen,
            onExpandedChange = { subjectDropdownOpen = it }
        ) {
            OutlinedTextField(
                value = uiState.selectedSubjectId?.let { id ->
                    uiState.availableSubjects.firstOrNull { it.id == id }?.name
                } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Materia") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectDropdownOpen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = subjectDropdownOpen,
                onDismissRequest = { subjectDropdownOpen = false }
            ) {
                uiState.availableSubjects.forEach { subject ->
                    DropdownMenuItem(
                        text = { Text("${subject.name} (${subject.degreeName} - Curso ${subject.courseYear})") },
                        onClick = {
                            viewModel.onSelectSubject(subject.id)
                            subjectDropdownOpen = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = topicDropdownOpen,
            onExpandedChange = { topicDropdownOpen = it }
        ) {
            OutlinedTextField(
                value = uiState.selectedTopicId?.let { id ->
                    uiState.topics.firstOrNull { it.id == id }?.name
                } ?: "Sin tema",
                onValueChange = {},
                readOnly = true,
                label = { Text("Tema (opcional)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicDropdownOpen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = topicDropdownOpen,
                onDismissRequest = { topicDropdownOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Sin tema") },
                    onClick = {
                        viewModel.onSelectTopic(null)
                        topicDropdownOpen = false
                    }
                )
                uiState.topics.forEach { topic ->
                    DropdownMenuItem(
                        text = { Text(topic.name) },
                        onClick = {
                            viewModel.onSelectTopic(topic.id)
                            topicDropdownOpen = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::onTitleChange,
            label = { Text("Título") },
            isError = uiState.titleError != null,
            supportingText = uiState.titleError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("Tipo", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MaterialType.entries.forEach { type ->
                FilterChip(
                    selected = uiState.type == type,
                    onClick = { viewModel.onTypeChange(type) },
                    label = { Text(type.displayName) }
                )
            }
        }

        OutlinedTextField(
            value = uiState.uri,
            onValueChange = viewModel::onUriChange,
            label = { Text("URI o enlace") },
            isError = uiState.uriError != null,
            supportingText = uiState.uriError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        Button(
            onClick = {
                filePicker.launch(
                    arrayOf(
                        "text/*",
                        "image/*",
                        "application/pdf"
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Seleccionar archivo local")
        }

        Text(
            text = "Solo se admiten texto, imagen con texto o PDF. Para estimaciones IA, se procesan hasta ${AiDocumentProcessingPolicy.MAX_PDF_PAGES_TEXT} paginas por documento (OCR hasta ${AiDocumentProcessingPolicy.MAX_PDF_PAGES_OCR}).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = viewModel::onSave,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (materialId == null) "Guardar material" else "Guardar cambios")
        }

        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Cancelar")
        }
    }
}
