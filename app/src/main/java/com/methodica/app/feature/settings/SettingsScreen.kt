package com.methodica.app.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.methodica.app.domain.ai.local.LocalModelDownloadPolicy
import com.methodica.app.domain.ai.local.LocalAiModelType
import com.methodica.app.domain.ai.local.LocalModelInstallState
import com.methodica.app.domain.ai.local.LocalModelInstallStatus
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var shouldShowPermissionDeniedMessage by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val requestNotificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.onRemindersEnabledChange(granted)
            if (!granted) {
                shouldShowPermissionDeniedMessage = true
            }
        }
    )

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbar.showSnackbar("Ajustes guardados")
            viewModel.onSavedConsumed()
        }
    }

    LaunchedEffect(uiState.aiConnectionResult) {
        val result = uiState.aiConnectionResult
        if (result != null) {
            snackbar.showSnackbar(result)
            viewModel.onConnectionResultConsumed()
        }
    }

    LaunchedEffect(uiState.aiConnectionError) {
        val error = uiState.aiConnectionError
        if (error != null) {
            snackbar.showSnackbar(error)
            viewModel.onConnectionResultConsumed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Ajustes de planificaciÃ³n", style = MaterialTheme.typography.headlineSmall)

        // Horas disponibles por dÃ­a
        Column {
            Text("Horas de estudio por dÃ­a: ${uiState.availableHoursPerDay}", style = MaterialTheme.typography.titleSmall)
            Slider(
                value         = uiState.availableHoursPerDay.toFloat(),
                onValueChange = { viewModel.onHoursPerDayChange(it.roundToInt()) },
                valueRange    = 1f..16f,
                steps         = 14,
                modifier      = Modifier.fillMaxWidth()
            )
        }

        // DÃ­as no disponibles
        Column {
            Text("DÃ­as no disponibles", style = MaterialTheme.typography.titleSmall)
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

        // ColchÃ³n antes del examen
        Column {
            Text("ColchÃ³n antes del examen: ${uiState.bufferDaysBeforeExam} dÃ­as", style = MaterialTheme.typography.titleSmall)
            Slider(
                value         = uiState.bufferDaysBeforeExam.toFloat(),
                onValueChange = { viewModel.onBufferDaysChange(it.roundToInt()) },
                valueRange    = 0f..14f,
                steps         = 13,
                modifier      = Modifier.fillMaxWidth()
            )
        }

        // DÃ­as de repaso final
        Column {
            Text("DÃ­as de repaso final: ${uiState.finalReviewDays}", style = MaterialTheme.typography.titleSmall)
            Slider(
                value         = uiState.finalReviewDays.toFloat(),
                onValueChange = { viewModel.onFinalReviewDaysChange(it.roundToInt()) },
                valueRange    = 0f..7f,
                steps         = 6,
                modifier      = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Recordatorios", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Notifica sesiones de hoy y evaluaciones prÃ³ximas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = uiState.remindersEnabled,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        viewModel.onRemindersEnabledChange(false)
                        return@Switch
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        viewModel.onRemindersEnabledChange(true)
                        return@Switch
                    }

                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        viewModel.onRemindersEnabledChange(true)
                    } else {
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Perfil de estudiante", style = MaterialTheme.typography.titleSmall)
            Text(
                "Este perfil se usa para personalizar la estimaciÃ³n IA de complejidad por tema.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column {
                Text(
                    "Edad: ${uiState.studentAge}",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = uiState.studentAge.toFloat(),
                    onValueChange = { viewModel.onStudentAgeChange(it.roundToInt()) },
                    valueRange = 10f..100f,
                    steps = 89,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column {
                Text(
                    "ComprensiÃ³n lectora: ${uiState.readingComprehensionLevel}/5",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = uiState.readingComprehensionLevel.toFloat(),
                    onValueChange = { viewModel.onReadingComprehensionLevelChange(it.roundToInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column {
                Text("Titulaciones en curso", style = MaterialTheme.typography.labelMedium)
                if (uiState.availableDegrees.isEmpty()) {
                    Text(
                        "No hay titulaciones creadas todavÃ­a.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.availableDegrees.forEach { degree ->
                            FilterChip(
                                selected = degree.id in uiState.studentCurrentDegreeIds,
                                onClick = { viewModel.onToggleCurrentDegree(degree.id) },
                                label = { Text(degree.name) }
                            )
                        }
                    }
                }
            }

            Column {
                Text("Titulaciones aprobadas", style = MaterialTheme.typography.labelMedium)
                if (uiState.availableDegrees.isEmpty()) {
                    Text(
                        "No hay titulaciones creadas todavÃ­a.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.availableDegrees.forEach { degree ->
                            FilterChip(
                                selected = degree.id in uiState.studentCompletedDegreeIds,
                                onClick = { viewModel.onToggleCompletedDegree(degree.id) },
                                label = { Text(degree.name) }
                            )
                        }
                    }
                }
            }

            Column {
                Text("Asignaturas aprobadas", style = MaterialTheme.typography.labelMedium)
                if (uiState.availablePassedSubjects.isEmpty()) {
                    Text(
                        "No hay asignaturas disponibles para las titulaciones en curso seleccionadas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.availablePassedSubjects.forEach { subject ->
                            FilterChip(
                                selected = subject.id in uiState.passedSubjectIds,
                                onClick = { viewModel.onTogglePassedSubject(subject.id) },
                                label = {
                                    Text("${subject.name} (C${subject.courseYear})")
                                }
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Modelos locales", style = MaterialTheme.typography.titleSmall)
            Text(
                "Methodica usa un manifest remoto de staging y guarda los modelos en almacenamiento privado. EmbeddingGemma se prepara automaticamente cuando falta; Gemma 3n solo se descarga bajo consentimiento explicito.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            uiState.localModelStates.forEach { modelState ->
                LocalModelStateCard(
                    state = modelState,
                    onDownload = { viewModel.onDownloadLocalModel(modelState.type) },
                    onCancel = { viewModel.onCancelLocalModelDownload(modelState.type) },
                    onDelete = { viewModel.onDeleteLocalModel(modelState.type) },
                    onReinstall = { viewModel.onReinstallLocalModel(modelState.type) },
                    onOpenUrl = uriHandler::openUri
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ConexiÃ³n IA externa", style = MaterialTheme.typography.titleSmall)
            Text(
                "Si usas este modo, consumirÃ¡s crÃ©ditos de tu proveedor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Activar IA externa", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Apaga esta opciÃ³n para forzar modo heurÃ­stico en planificaciÃ³n.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.aiExternalEnabled,
                    onCheckedChange = viewModel::onAiExternalEnabledChange
                )
            }

            if (!uiState.aiExternalEnabled) return@Column

            // ðŸ†• DROPDOWN DE PROVEEDOR
            Column {
                Button(
                    onClick = viewModel::onToggleProviderDropdown,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(
                        uiState.selectedProviderPreset?.displayName ?: "Selecciona proveedor",
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                    Text("â–¼", modifier = Modifier.padding(start = 8.dp))
                }

                DropdownMenu(
                    expanded = uiState.showProviderDropdown,
                    onDismissRequest = viewModel::onToggleProviderDropdown,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    uiState.availableProviders.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.displayName) },
                            onClick = { viewModel.onSelectAiProvider(provider) }
                        )
                    }
                }
            }

            if (uiState.selectedProviderPreset != null) {
                OutlinedTextField(
                    value = uiState.aiModel,
                    onValueChange = viewModel::onAiModelChange,
                    label = { Text("Modelo") },
                    placeholder = { Text("Introduce el ID del modelo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (uiState.availableModels.isNotEmpty()) {
                    Column {
                        Button(
                            onClick = viewModel::onToggleModelDropdown,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(
                                "Elegir modelo sugerido",
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )
                            Text("â–¼", modifier = Modifier.padding(start = 8.dp))
                        }

                        DropdownMenu(
                            expanded = uiState.showModelDropdown,
                            onDismissRequest = viewModel::onToggleModelDropdown,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            uiState.availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = { viewModel.onSelectAiModel(model) }
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    "Selecciona un proveedor para habilitar el modelo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // URL Base (solo lectura, mapeada automÃ¡ticamente)
            OutlinedTextField(
                value = uiState.aiBaseUrl,
                onValueChange = {},
                label = { Text("Base URL (automÃ¡tica)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = false
            )

            // API Key
            OutlinedTextField(
                value = uiState.aiApiKey,
                onValueChange = viewModel::onAiApiKeyChange,
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "DocumentaciÃ³n de proveedores",
                    style = MaterialTheme.typography.titleSmall
                )
                uiState.availableProviders.forEach { provider ->
                    TextButton(
                        onClick = { uriHandler.openUri(provider.documentation) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "${provider.displayName}: modelos y API",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = viewModel::onTestAiConnection,
                    enabled = !uiState.isTestingAiConnection && uiState.selectedProviderPreset != null,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isTestingAiConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.then(Modifier.size(20.dp)),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Probar conexiÃ³n")
                    }
                }
            }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalModelStateCard(
    state: LocalModelInstallState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onReinstall: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val cardColors = when (state.type) {
        LocalAiModelType.EMBEDDING_GEMMA -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
        LocalAiModelType.GEMMA_3N_REASONING -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
        else -> CardDefaults.cardColors()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.policyLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    state.statusLabel(),
                    style = MaterialTheme.typography.labelLarge,
                    color = state.statusColor()
                )
            }
            Text(
                state.usageSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                buildString {
                    append("Tamano aprox.: ")
                    append(formatBytes((if (state.totalBytes > 0L) state.totalBytes else state.requiredDiskBytes)))
                    append(" • Espacio recomendado: ")
                    append(formatBytes(state.requiredDiskBytes))
                    append(" • RAM minima: ")
                    append(state.requiredRamMb)
                    append(" MB")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.supportedAbis.isNotEmpty()) {
                Text(
                    "Compatibilidad ABI: ${state.supportedAbis.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.recommendedOnWifi) {
                Text(
                    "Recomendacion: usa Wi-Fi estable antes de descargar este modelo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.type == LocalAiModelType.GEMMA_3N_REASONING) {
                Text(
                    "Uso local avanzado: solo disponible cuando el dispositivo pueda ejecutar Gemma 3n y el usuario lo haya aceptado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.progressPercent?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Progreso: $progress% (${formatBytes(state.downloadedBytes)} / ${formatBytes(state.totalBytes)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.lastError?.takeIf { it.isNotBlank() }?.let { detail ->
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = state.statusMessageColor()
                )
            }
            if (state.noticeUrl != null || state.termsUrl != null || state.prohibitedUsePolicyUrl != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Avisos legales",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.noticeUrl?.let { url ->
                            TextButton(onClick = { onOpenUrl(url) }) {
                                Text("NOTICE")
                            }
                        }
                        state.termsUrl?.let { url ->
                            TextButton(onClick = { onOpenUrl(url) }) {
                                Text("Gemma Terms")
                            }
                        }
                        state.prohibitedUsePolicyUrl?.let { url ->
                            TextButton(onClick = { onOpenUrl(url) }) {
                                Text("Use Policy")
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (state.status) {
                    LocalModelInstallStatus.DOWNLOADING,
                    LocalModelInstallStatus.VERIFYING,
                    LocalModelInstallStatus.INSTALLING -> {
                        Button(onClick = onCancel) {
                            Text("Cancelar")
                        }
                    }
                    LocalModelInstallStatus.READY -> {
                        Button(onClick = onReinstall, enabled = state.isDownloadConfigured) {
                            Text("Reinstalar")
                        }
                        TextButton(onClick = onDelete) {
                            Text("Borrar")
                        }
                    }
                    LocalModelInstallStatus.NOT_INSTALLED,
                    LocalModelInstallStatus.ERROR,
                    LocalModelInstallStatus.NO_SPACE,
                    LocalModelInstallStatus.INCOMPATIBLE_DEVICE -> {
                        Button(onClick = onDownload, enabled = state.isDownloadConfigured) {
                            Text(state.primaryActionLabel())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalModelInstallState.statusColor() = when (status) {
    LocalModelInstallStatus.READY -> MaterialTheme.colorScheme.primary
    LocalModelInstallStatus.DOWNLOADING,
    LocalModelInstallStatus.VERIFYING,
    LocalModelInstallStatus.INSTALLING -> MaterialTheme.colorScheme.tertiary
    LocalModelInstallStatus.ERROR,
    LocalModelInstallStatus.NO_SPACE,
    LocalModelInstallStatus.INCOMPATIBLE_DEVICE -> MaterialTheme.colorScheme.error
    LocalModelInstallStatus.NOT_INSTALLED -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun LocalModelInstallState.statusMessageColor() = when (status) {
    LocalModelInstallStatus.ERROR,
    LocalModelInstallStatus.NO_SPACE,
    LocalModelInstallStatus.INCOMPATIBLE_DEVICE -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun LocalModelInstallState.statusLabel(): String = when (status) {
    LocalModelInstallStatus.NOT_INSTALLED -> "Falta"
    LocalModelInstallStatus.DOWNLOADING -> "Descargando"
    LocalModelInstallStatus.VERIFYING -> "Verificando"
    LocalModelInstallStatus.INSTALLING -> "Instalando"
    LocalModelInstallStatus.READY -> "Listo"
    LocalModelInstallStatus.ERROR -> "Error"
    LocalModelInstallStatus.NO_SPACE -> "Sin espacio"
    LocalModelInstallStatus.INCOMPATIBLE_DEVICE -> "No compatible"
}

private fun LocalModelInstallState.policyLabel(): String = when (downloadPolicy) {
    LocalModelDownloadPolicy.AUTOMATIC -> "Descarga automatica en segundo plano"
    LocalModelDownloadPolicy.EXPLICIT_USER_ACTION -> "Descarga bajo consentimiento"
}

private fun LocalModelInstallState.primaryActionLabel(): String = when {
    status == LocalModelInstallStatus.NOT_INSTALLED && downloadPolicy == LocalModelDownloadPolicy.EXPLICIT_USER_ACTION ->
        "Descargar manualmente"
    status == LocalModelInstallStatus.NOT_INSTALLED ->
        "Descargar"
    else -> "Reintentar"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "Pendiente"
    val gb = 1024L * 1024L * 1024L
    val mb = 1024L * 1024L
    return when {
        bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes.toDouble() / gb.toDouble())
        else -> String.format(Locale.US, "%.0f MB", bytes.toDouble() / mb.toDouble())
    }
}




