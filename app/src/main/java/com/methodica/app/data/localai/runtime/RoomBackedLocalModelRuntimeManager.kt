package com.methodica.app.data.localai.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.methodica.app.data.local.dao.AiIndexingRunDao
import com.methodica.app.data.local.dao.LocalAiModelStateDao
import com.methodica.app.data.local.entity.LocalAiModelStateEntity
import com.methodica.app.domain.ai.local.DeviceCompatibilityReport
import com.methodica.app.domain.ai.local.LocalAiModelSpec
import com.methodica.app.domain.ai.local.LocalAiModelType
import com.methodica.app.domain.ai.local.LocalModelRuntimeManager
import com.methodica.app.domain.ai.local.LocalModelRuntimeState
import com.methodica.app.domain.ai.local.RuntimeAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class RoomBackedLocalModelRuntimeManager @Inject constructor(
    private val modelStateDao: LocalAiModelStateDao,
    @ApplicationContext private val context: Context,
    private val aiIndexingRunDao: AiIndexingRunDao
) : LocalModelRuntimeManager {

    private val modelInitMutex = Mutex()

    override fun observeRuntimeState(): Flow<LocalModelRuntimeState> =
        combine(modelStateDao.observeAll(), aiIndexingRunDao.observeRunningCount()) { records, runningCount ->
            val installed = records
                .filter { it.status == STATUS_READY }
                .mapNotNull { runCatching { LocalAiModelType.valueOf(it.modelType) }.getOrNull() }
                .toSet()

            val activeStatus = records.maxByOrNull { it.updatedAt }?.status
            val availability = when {
                activeStatus == STATUS_DOWNLOADING -> RuntimeAvailability.DOWNLOADING
                activeStatus == STATUS_INITIALIZING -> RuntimeAvailability.INITIALIZING
                records.any { it.status == STATUS_ERROR } -> RuntimeAvailability.ERROR
                installed.isNotEmpty() -> RuntimeAvailability.READY
                else -> RuntimeAvailability.UNINITIALIZED
            }
            val runtimeMessage = records.maxByOrNull { it.updatedAt }?.let { state ->
                when (state.status) {
                    STATUS_MISSING_MODEL -> "Modelo local no disponible. Descárgalo o instálalo manualmente."
                    STATUS_DOWNLOADING -> "Descargando modelo local (${state.modelId})…"
                    STATUS_INITIALIZING -> "Inicializando runtime del modelo local…"
                    STATUS_INCOMPATIBLE -> state.lastError ?: "Dispositivo no compatible con el modelo local"
                    STATUS_NO_SPACE -> state.lastError ?: "Espacio insuficiente para instalar el modelo local"
                    STATUS_INTEGRITY_ERROR -> state.lastError ?: "Falló la verificación de integridad del modelo"
                    STATUS_ERROR -> state.lastError
                    else -> null
                }
            }

            LocalModelRuntimeState(
                availability = availability,
                isIndexing = runningCount > 0,
                installedModels = installed,
                lastError = runtimeMessage
            )
        }

    override suspend fun evaluateDeviceCompatibility(spec: LocalAiModelSpec): DeviceCompatibilityReport {
        val statFs = StatFs(context.filesDir.absolutePath)
        val availableDisk = statFs.availableBytes
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val availableRamMb = activityManager.memoryClass

        val blockers = buildList {
            if (!Build.SUPPORTED_64_BIT_ABIS.any { it.contains("arm64") || it.contains("x86_64") }) {
                add("ABI no compatible: se requiere arquitectura 64-bit para ${spec.id}.")
            }
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
        modelInitMutex.withLock {
            persistState(spec = spec, status = STATUS_INITIALIZING, lastError = null)
            val compatibility = evaluateDeviceCompatibility(spec)
            if (!compatibility.isSupported) {
                val reason = compatibility.blockers.joinToString(" | ")
                val status = when {
                    reason.contains("Espacio insuficiente") -> STATUS_NO_SPACE
                    reason.contains("ABI no compatible") -> STATUS_INCOMPATIBLE
                    else -> STATUS_ERROR
                }
                persistState(spec = spec, status = status, lastError = reason)
                error(reason)
            }

            val modelFile = context.filesDir.resolve(spec.localRelativePath)
            if (!modelFile.exists()) {
                val installedFromAssets = installFromAssetsIfPresent(spec, modelFile)
                if (!installedFromAssets) {
                    val downloaded = downloadModelIfConfigured(spec, modelFile)
                    if (!downloaded) {
                        persistState(
                            spec,
                            STATUS_MISSING_MODEL,
                            "No se encontró ${spec.assetPath} en assets y no hay URL de descarga configurada."
                        )
                        error("Modelo no disponible localmente")
                    }
                }
            }

            verifyIntegrity(spec, modelFile)
            persistState(spec = spec, status = STATUS_READY, lastError = null)
        }
    }

    override suspend fun releaseModels() {
        // El provider de embeddings mantiene la instancia del runtime y la libera en GC.
    }

    private suspend fun installFromAssetsIfPresent(spec: LocalAiModelSpec, destination: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.assets.open(spec.assetPath).use { input ->
                destination.parentFile?.mkdirs()
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            }
            true
        }.getOrDefault(false)
    }

    private suspend fun downloadModelIfConfigured(spec: LocalAiModelSpec, destination: File): Boolean {
        val downloadUrl = spec.downloadUrl ?: return false
        return withContext(Dispatchers.IO) {
            runCatching {
                persistState(spec, STATUS_DOWNLOADING, null)
                destination.parentFile?.mkdirs()
                val temp = File(destination.parentFile, "${destination.name}.download")
                val connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    requestMethod = "GET"
                }
                connection.inputStream.use { input ->
                    FileOutputStream(temp).use { output -> input.copyTo(output) }
                }
                if (destination.exists()) destination.delete()
                temp.renameTo(destination)
                true
            }.getOrElse {
                persistState(spec, STATUS_ERROR, "Fallo descarga del modelo: ${it.message}")
                false
            }
        }
    }

    private suspend fun verifyIntegrity(spec: LocalAiModelSpec, modelFile: File) {
        if (!modelFile.exists()) {
            persistState(spec, STATUS_MISSING_MODEL, "No existe archivo del modelo en ${modelFile.absolutePath}")
            error("Archivo de modelo no encontrado")
        }
        val expected = spec.expectedSha256 ?: return
        val actual = sha256(modelFile)
        if (!actual.equals(expected, ignoreCase = true)) {
            persistState(spec, STATUS_INTEGRITY_ERROR, "SHA-256 inválido para ${spec.id}. Esperado=$expected actual=$actual")
            error("Integridad del modelo inválida")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun persistState(spec: LocalAiModelSpec, status: String, lastError: String?) {
        modelStateDao.upsert(
            LocalAiModelStateEntity(
                modelType = spec.type.name,
                modelId = spec.id,
                modelVersion = spec.version,
                status = status,
                localPath = spec.localRelativePath,
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
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_INITIALIZING = "INITIALIZING"
        const val STATUS_MISSING_MODEL = "MISSING_MODEL"
        const val STATUS_INCOMPATIBLE = "INCOMPATIBLE_DEVICE"
        const val STATUS_NO_SPACE = "NO_SPACE"
        const val STATUS_INTEGRITY_ERROR = "INTEGRITY_ERROR"
    }
}
