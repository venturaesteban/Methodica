package com.methodica.app.data.localai.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import com.methodica.app.data.local.dao.LocalAiModelStateDao
import com.methodica.app.data.local.entity.LocalAiModelStateEntity
import com.methodica.app.domain.ai.local.DeviceCompatibilityReport
import com.methodica.app.domain.ai.local.LocalAiModelSpec
import com.methodica.app.domain.ai.local.LocalAiModelType
import com.methodica.app.domain.ai.local.LocalModelRuntimeManager
import com.methodica.app.domain.ai.local.LocalModelRuntimeState
import com.methodica.app.domain.ai.local.RuntimeAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomBackedLocalModelRuntimeManager @Inject constructor(
    private val modelStateDao: LocalAiModelStateDao,
    @ApplicationContext private val context: Context
) : LocalModelRuntimeManager {

    override fun observeRuntimeState(): Flow<LocalModelRuntimeState> =
        modelStateDao.observeAll().map { records ->
            val installed = records
                .filter { it.status == STATUS_READY }
                .mapNotNull { runCatching { LocalAiModelType.valueOf(it.modelType) }.getOrNull() }
                .toSet()

            val anyError = records.firstOrNull { it.status == STATUS_ERROR }
            LocalModelRuntimeState(
                availability = when {
                    anyError != null -> RuntimeAvailability.ERROR
                    installed.isNotEmpty() -> RuntimeAvailability.READY
                    else -> RuntimeAvailability.UNINITIALIZED
                },
                isIndexing = false,
                installedModels = installed,
                lastError = anyError?.lastError
            )
        }

    override suspend fun evaluateDeviceCompatibility(spec: LocalAiModelSpec): DeviceCompatibilityReport {
        val statFs = StatFs(context.filesDir.absolutePath)
        val availableDisk = statFs.availableBytes
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val availableRamMb = activityManager.memoryClass

        val blockers = buildList {
            if (availableDisk < spec.requiredDiskBytes) {
                add("Espacio insuficiente para ${spec.id}. Requiere ${spec.requiredDiskBytes} bytes.")
            }
            if (availableRamMb < spec.requiredRamMb) {
                add("RAM insuficiente para ${spec.id}. Requiere ${spec.requiredRamMb} MB.")
            }
        }
        return DeviceCompatibilityReport(
            isSupported = blockers.isEmpty(),
            availableDiskBytes = availableDisk,
            availableRamMb = availableRamMb,
            blockers = blockers
        )
    }

    override suspend fun ensureModelReady(spec: LocalAiModelSpec): Result<Unit> = runCatching {
        val compatibility = evaluateDeviceCompatibility(spec)
        if (!compatibility.isSupported) {
            val reason = compatibility.blockers.joinToString(" | ")
            persistState(spec = spec, status = STATUS_ERROR, lastError = reason)
            error(reason)
        }
        persistState(spec = spec, status = STATUS_READY, lastError = null)
    }

    override suspend fun releaseModels() {
        // Scaffolding: en fase 3 se cerrará runtime nativo real (NNAPI/GPU delegates).
    }

    private suspend fun persistState(spec: LocalAiModelSpec, status: String, lastError: String?) {
        modelStateDao.upsert(
            LocalAiModelStateEntity(
                modelType = spec.type.name,
                modelId = spec.id,
                modelVersion = spec.version,
                status = status,
                localPath = spec.assetPath,
                requiredDiskBytes = spec.requiredDiskBytes,
                requiredRamMb = spec.requiredRamMb,
                lastError = lastError,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private companion object {
        const val STATUS_READY = "READY"
        const val STATUS_ERROR = "ERROR"
    }
}
