package com.methodica.app.feature.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.methodica.app.AppContainer

private val PRESET_COLORS = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#3F51B5",
    "#2196F3", "#009688", "#4CAF50", "#FF9800"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectFormScreen(
    subjectId:      Long?,
    container:      AppContainer,
    onNavigateBack: () -> Unit
) {
    val viewModel: SubjectFormViewModel = viewModel(
        key     = "subjectForm_$subjectId",
        factory = SubjectFormViewModel.factory(subjectId, container)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navegar atrás en cuanto el guardado se confirma
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
            value         = uiState.name,
            onValueChange = viewModel::onNameChange,
            label         = { Text("Nombre *") },
            isError       = uiState.nameError != null,
            supportingText = uiState.nameError?.let { { Text(it) } },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true
        )

        Text("Color", style = MaterialTheme.typography.labelMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp)
        ) {
            PRESET_COLORS.forEach { hex ->
                val isSelected = uiState.colorHex == hex
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(parseFormColor(hex))
                        .then(
                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { viewModel.onColorChange(hex) }
                )
            }
        }

        OutlinedTextField(
            value         = uiState.description,
            onValueChange = viewModel::onDescriptionChange,
            label         = { Text("Descripción (opcional)") },
            modifier      = Modifier.fillMaxWidth(),
            minLines      = 2,
            maxLines      = 4
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
            Text(if (subjectId != null) "Guardar cambios" else "Crear materia")
        }

        TextButton(
            onClick  = onNavigateBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Cancelar")
        }
    }
}

private fun parseFormColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color.Gray)
