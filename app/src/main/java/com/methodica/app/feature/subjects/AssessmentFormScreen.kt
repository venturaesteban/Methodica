package com.methodica.app.feature.subjects

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.methodica.app.AppContainer
import com.methodica.app.domain.model.AssessmentType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssessmentFormScreen(
    subjectId:      Long,
    assessmentId:   Long?,
    container:      AppContainer,
    onNavigateBack: () -> Unit
) {
    val viewModel: AssessmentFormViewModel = viewModel(
        key     = "assessmentForm_${subjectId}_$assessmentId",
        factory = AssessmentFormViewModel.factory(subjectId, assessmentId, container)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    var showDatePicker  by remember { mutableStateOf(false) }
    var typeDropdownOpen by remember { mutableStateOf(false) }
    val datePickerState  = rememberDatePickerState(initialSelectedDateMillis = uiState.date)
    val dateFormatter    = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton    = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tipo de evaluación (dropdown)
        ExposedDropdownMenuBox(
            expanded        = typeDropdownOpen,
            onExpandedChange = { typeDropdownOpen = it }
        ) {
            OutlinedTextField(
                value         = uiState.type.displayName,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Tipo") },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownOpen) },
                modifier      = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded        = typeDropdownOpen,
                onDismissRequest = { typeDropdownOpen = false }
            ) {
                AssessmentType.entries.forEach { type ->
                    DropdownMenuItem(
                        text    = { Text(type.displayName) },
                        onClick = {
                            viewModel.onTypeChange(type)
                            typeDropdownOpen = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value          = uiState.title,
            onValueChange  = viewModel::onTitleChange,
            label          = { Text("Título *") },
            isError        = uiState.titleError != null,
            supportingText = uiState.titleError?.let { { Text(it) } },
            modifier       = Modifier.fillMaxWidth(),
            singleLine     = true
        )

        // Campo de fecha (solo lectura, abre DatePickerDialog)
        OutlinedTextField(
            value         = remember(uiState.date) { dateFormatter.format(Date(uiState.date)) },
            onValueChange = {},
            readOnly      = true,
            label         = { Text("Fecha") },
            trailingIcon  = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Seleccionar fecha")
                }
            },
            modifier      = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value          = uiState.weight,
            onValueChange  = viewModel::onWeightChange,
            label          = { Text("Peso % (0-100, opcional)") },
            isError        = uiState.weightError != null,
            supportingText = uiState.weightError?.let { { Text(it) } },
            modifier       = Modifier.fillMaxWidth(),
            singleLine     = true
        )

        OutlinedTextField(
            value         = uiState.notes,
            onValueChange = viewModel::onNotesChange,
            label         = { Text("Notas (opcional)") },
            modifier      = Modifier.fillMaxWidth(),
            minLines      = 2,
            maxLines      = 4
        )

        // Selector de temas asociados
        if (uiState.availableTopics.isNotEmpty()) {
            Column {
                Text("Temas asociados", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.availableTopics.forEach { topic ->
                        FilterChip(
                            selected = topic.id in uiState.selectedTopicIds,
                            onClick  = { viewModel.onToggleTopic(topic.id) },
                            label    = { Text(topic.name) }
                        )
                    }
                }
            }
        }

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick  = viewModel::onSave,
            enabled  = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (assessmentId != null) "Guardar cambios" else "Crear evaluación")
        }

        TextButton(
            onClick  = onNavigateBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Cancelar")
        }
    }
}
