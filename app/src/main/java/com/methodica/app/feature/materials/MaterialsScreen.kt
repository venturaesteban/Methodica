package com.methodica.app.feature.materials

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.methodica.app.domain.model.Material
import com.methodica.app.domain.model.MaterialType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsScreen(
    onNavigateToForm: (Long?) -> Unit,
    viewModel: MaterialsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Material?>(null) }
    var degreeDropdownOpen by remember { mutableStateOf(false) }
    var courseDropdownOpen by remember { mutableStateOf(false) }
    var subjectDropdownOpen by remember { mutableStateOf(false) }

    fun openMaterial(uriValue: String) {
        val uri = uriValue.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
            .onFailure {
                val message = if (it is ActivityNotFoundException) {
                    "No hay una app para abrir este material"
                } else {
                    "No se pudo abrir el material"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToForm(null) }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir material")
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Materiales", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = degreeDropdownOpen,
                onExpandedChange = { degreeDropdownOpen = it }
            ) {
                OutlinedTextField(
                    value = uiState.selectedDegreeId?.let { selectedId ->
                        uiState.availableDegrees.firstOrNull { it.first == selectedId }?.second
                    } ?: "Todas las titulaciones",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filtrar por titulacion") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = degreeDropdownOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = degreeDropdownOpen,
                    onDismissRequest = { degreeDropdownOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todas las titulaciones") },
                        onClick = {
                            viewModel.onFilterDegree(null)
                            degreeDropdownOpen = false
                        }
                    )
                    uiState.availableDegrees.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                viewModel.onFilterDegree(id)
                                degreeDropdownOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = courseDropdownOpen,
                onExpandedChange = { courseDropdownOpen = it }
            ) {
                OutlinedTextField(
                    value = uiState.selectedCourseYear?.let { "Curso $it" } ?: "Todos los cursos",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filtrar por curso") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = courseDropdownOpen,
                    onDismissRequest = { courseDropdownOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todos los cursos") },
                        onClick = {
                            viewModel.onFilterCourseYear(null)
                            courseDropdownOpen = false
                        }
                    )
                    uiState.availableCourseYears.forEach { year ->
                        DropdownMenuItem(
                            text = { Text("Curso $year") },
                            onClick = {
                                viewModel.onFilterCourseYear(year)
                                courseDropdownOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = subjectDropdownOpen,
                onExpandedChange = { subjectDropdownOpen = it }
            ) {
                OutlinedTextField(
                    value = uiState.selectedSubjectId?.let { selectedId ->
                        uiState.availableSubjects.firstOrNull { it.id == selectedId }?.name
                    } ?: "Todas las asignaturas",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filtrar por asignatura") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectDropdownOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = subjectDropdownOpen,
                    onDismissRequest = { subjectDropdownOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todas las asignaturas") },
                        onClick = {
                            viewModel.onFilterSubject(null)
                            subjectDropdownOpen = false
                        }
                    )
                    uiState.availableSubjects.forEach { subject ->
                        DropdownMenuItem(
                            text = { Text(subject.name) },
                            onClick = {
                                viewModel.onFilterSubject(subject.id)
                                subjectDropdownOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { viewModel.onFilterType(null) },
                    label = { Text("Todos") }
                )
                MaterialType.entries.forEach { type ->
                    AssistChip(
                        onClick = { viewModel.onFilterType(type) },
                        label = { Text(type.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (uiState.filteredMaterials.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No hay materiales para este filtro",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Pulsa + para añadir enlaces o archivos locales",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(uiState.filteredMaterials, key = { index, material -> "material-${material.id}-$index" }) { _, material ->
                        val subject = uiState.subjects.firstOrNull { it.id == material.subjectId }
                        val subjectName = subject?.name ?: "Sin asignatura"
                        val hierarchy = if (subject != null) {
                            "${subject.degreeName} - Curso ${subject.courseYear}"
                        } else {
                            "Sin contexto academico"
                        }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(material.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "$subjectName • $hierarchy • ${material.type.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    material.uri,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = { openMaterial(material.uri) }) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Abrir")
                                    }
                                    IconButton(onClick = { onNavigateToForm(material.id) }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                                    }
                                    IconButton(onClick = { pendingDelete = material }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    pendingDelete?.let { material ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar material") },
            text = { Text("¿Eliminar \"${material.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteMaterial(material)
                    pendingDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }
}
