package com.methodica.app.data.localai.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.methodica.app.data.local.dao.AiIndexingRunDao
import com.methodica.app.data.local.dao.LocalAiModelStateDao
import com.methodica.app.data.local.entity.LocalAiModelStateEntity
import com.methodica.app.domain.ai.local.DeviceCompatibilityReport
import com.methodica.app.domain.ai.local.DownloadableLocalModelDescriptor
import com.methodica.app.domain.ai.local.LocalAiModelSpec
import com.methodica.app.domain.ai.local.LocalAiModelType
import com.methodica.app.domain.ai.local.LocalModelDownloadPolicy
import com.methodica.app.domain.ai.local.LocalModelInstallState
import com.methodica.app.domain.ai.local.LocalModelRuntimeManager
import com.methodica.app.domain.ai.local.LocalModelRuntimeState
import com.methodica.app.domain.ai.local.RuntimeAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class RoomBackedLocalModelRuntimeManager @Inject constructor(
    private val modelStateDao: LocalAiModelStateDao,
    @ApplicationContext private val context: Context,
    private val aiIndexingRunDao: AiIndexingRunDao,
    private val distributionResolver: LocalModelDistributionResolver,
    private val downloadScheduler: LocalModelDownloadScheduler
) : LocalModelRuntimeManager {

    private val modelInitMutex = Mutex()

    override fun observeRuntimeState(): Flow<LocalModelRuntimeState> =
        combine(modelStateDao.observeAll(), aiIndexingRunDao.observeRunningCount()) { records, runningCount ->
            val installed = records
                .filter { it.status == STATUS_READY }
                .mapNotNull { runCatching { LocalAiModelType.valueOf(it.modelType) }.getOrNull() }
                .toSet()

            val hasDownloading = records.any { it.status == STATUS_DOWNLOADING }
            val hasInstalling = records.any { it.status in setOf(STATUS_VERIFYING, STATUS_INSTALLING, STATUS_INITIALIZING) }
            val hasErrors = records.any { it.status in TERMINAL_ERROR_STATUSES }
            val latest = records.maxByOrNull { it.updatedAt }

            val availability = when {
                hasDownloading -> RuntimeAvailability.DOWNLOADING
                hasInstalling -> RuntimeAvailability.INITIALIZING
                installed.isNotEmpty() -> RuntimeAvailability.READY
                hasErrors -> RuntimeAvailability.ERROR
                else -> RuntimeAvailability.UNINITIALIZED
            }

            LocalModelRuntimeState(
                availability = availability,
                isIndexing = runningCount > 0,
                installedModels = installed,
                lastError = latest?.toRuntimeMessage()
            )
        }

    override fun observeModelInstallStates(): Flow<List<LocalModelInstallState>> =
        modelStateDao.observeAll().map { rows ->
            val byType = rows.associateBy { row -> row.modelType }
            LocalAiModelCatalog.allDefinitions.map { definition ->
                byType[definition.spec.type.name]?.toInstallState(definition) ?: LocalModelInstallState(
                    type = definition.spec.type,
                    modelId = definition.spec.id,
                    displayName = definition.spec.displayName,
                    modelVersion = definition.spec.version,
                    downloadPolicy = definition.spec.downloadPolicy,
                    usageSummary = definition.spec.usageSummary,
                    status = com.methodica.app.domain.ai.local.LocalModelInstallStatus.NOT_INSTALLED,
                    localRelativePath = definition.spec.localRelativePath,
                    requiredDiskBytes = definition.fallbackDownload?.requiredDiskBytes ?: definition.spec.requiredDiskBytes,
                    requiredRamMb = definition.fallbackDownload?.requiredRamMb ?: definition.spec.requiredRamMb,
                    supportedAbis = definition.fallbackDownload?.supportedAbis.orEmpty(),
                    minSdk = definition.fallbackDownload?.minSdk ?: Build.VERSION.SDK_INT,
                    recommendedOnWifi = definition.spec.recommendedOnWifi,
                    noticeUrl = definition.fallbackDownload?.noticeUrl ?: definition.spec.defaultNoticeUrl,
                    termsUrl = definition.fallbackDownload?.termsUrl ?: definition.spec.defaultTermsUrl,
                    prohibitedUsePolicyUrl = definition.fallbackDownload?.prohibitedUsePolicyUrl
                        ?: definition.spec.defaultProhibitedUsePolicyUrl,
                    downloadedBytes = 0L,
                    totalBytes = definition.fallbackDownload?.sizeBytes ?: 0L,
                    lastError = null,
                    updatedAt = 0L,
                    isDownloadConfigured = definition.fallbackDownload != null,
                    redistributionRequiresLicenseConfirmation = definition.spec.redistributionRequiresLicenseConfirmation
                )
            }
        }

    override suspend fun evaluateDeviceCompatibility(spec: LocalAiModelSpec): DeviceCompatibilityReport {
        val resolved = distributionResolver.resolve(spec.type)
        return evaluateDeviceCompatibility(spec, resolved.descriptor)
    }

    override suspend fun refreshDownloadableModels(): Result<Unit> = runCatching {
        distributionResolver.resolveAll().forEach { resolved ->
            val existing = modelStateDao.getByModelType(resolved.definition.spec.type.name)
            val currentStatus = existing?.status ?: if (resolved.resolutionError == null) STATUS_NOT_INSTALLED else STATUS_ERROR
            val mergedError = when {
                resolved.descriptor != null && existing?.downloadUrl.isNullOrBlank() -> null
                else -> existing?.lastError ?: resolved.resolutionError
            }
            persistState(
                definition = resolved.definition,
                descriptor = resolved.descriptor,
                status = currentStatus,
                lastError = mergedError,
                downloadedBytes = existing?.downloadedBytes ?: 0L,
                totalBytes = existing?.totalBytes ?: resolved.descriptor?.sizeBytes ?: 0L
            )
        }
    }

    override suspend fun prepareAutomaticModels(): Result<Unit> = runCatching {
        refreshDownloadableModels().getOrThrow()
        LocalAiModelCatalog.allDefinitions
            .filter { it.spec.downloadPolicy == LocalModelDownloadPolicy.AUTOMATIC }
            .forEach { definition ->
                val existing = modelStateDao.getByModelType(definition.spec.type.name)
                if (existing?.status in setOf(STATUS_DOWNLOADING, STATUS_VERIFYING, STATUS_INSTALLING, STATUS_INITIALIZING)) {
                    return@forEach
                }
                runCatching { ensureModelReady(definition.spec).getOrThrow() }
            }
    }

    override suspend fun requestModelDownload(type: LocalAiModelType): Result<Unit> = runCatching {
        val resolved = distributionResolver.resolve(type)
        val descriptor = resolved.descriptor
        if (descriptor == null) {
            persistState(
                definition = resolved.definition,
                descriptor = null,
                status = STATUS_ERROR,
                lastError = resolved.resolutionError,
                downloadedBytes = 0L,
                totalBytes = 0L
            )
            error(resolved.resolutionError ?: "No hay distribucion remota configurada para ${resolved.definition.spec.displayName}.")
        }

        val compatibility = evaluateDeviceCompatibility(resolved.definition.spec, descriptor)
        if (!compatibility.isSupported) {
            val reason = compatibility.blockers.joinToString(" | ")
            val status = if (reason.contains("Espacio insuficiente", ignoreCase = true)) STATUS_NO_SPACE else STATUS_INCOMPATIBLE
            persistState(
                definition = resolved.definition,
                descriptor = descriptor,
                status = status,
                lastError = reason,
                downloadedBytes = 0L,
                totalBytes = descriptor.sizeBytes
            )
            error(reason)
        }

        persistState(
            definition = resolved.definition,
            descriptor = descriptor,
            status = STATUS_DOWNLOADING,
            lastError = null,
            downloadedBytes = 0L,
            totalBytes = descriptor.sizeBytes
        )
        downloadScheduler.enqueue(type)
    }

    override suspend fun cancelModelDownload(type: LocalAiModelType): Result<Unit> = runCatching {
        downloadScheduler.cancel(type)
        val definition = LocalAiModelCatalog.definitionFor(type)
        val existing = modelStateDao.getByModelType(type.name)
        persistState(
            definition = definition,
            descriptor = existing?.toDescriptor(),
            status = STATUS_NOT_INSTALLED,
            lastError = "Descarga cancelada por el usuario.",
            downloadedBytes = 0L,
            totalBytes = existing?.totalBytes ?: 0L
        )
    }

    override suspend fun deleteInstalledModel(type: LocalAiModelType): Result<Unit> = runCatching {
        downloadScheduler.cancel(type)
        val definition = LocalAiModelCatalog.definitionFor(type)
        val existing = modelStateDao.getByModelType(type.name)
        val modelFile = context.filesDir.resolve(definition.spec.localRelativePath)
        modelFile.delete()
        modelFile.parentFile?.listFiles()?.forEach { file ->
            if (file.name.endsWith(".download", ignoreCase = true) || file.isFile) {
                file.delete()
            }
        }
        persistState(
            definition = definition,
            descriptor = existing?.toDescriptor() ?: definition.fallbackDownload,
            status = STATUS_NOT_INSTALLED,
            lastError = null,
            downloadedBytes = 0L,
            totalBytes = existing?.totalBytes ?: definition.fallbackDownload?.sizeBytes ?: 0L
        )
    }

    override suspend fun ensureModelReady(spec: LocalAiModelSpec): Result<Unit> = runCatching {
        modelInitMutex.withLock {
            val definition = LocalAiModelCatalog.definitionFor(spec.type)
            val modelFile = context.filesDir.resolve(spec.localRelativePath)
            val existing = modelStateDao.getByModelType(spec.type.name)
            val existingDescriptor = existing?.toDescriptor() ?: definition.fallbackDownload

            if (existing?.modelVersion?.isNotBlank() == true && existing.modelVersion != spec.version) {
                modelFile.delete()
                if (definition.spec.downloadPolicy == LocalModelDownloadPolicy.AUTOMATIC) {
                    requestModelDownload(spec.type).getOrThrow()
                    error("Version de ${spec.displayName} obsoleta. Se ha programado la reinstalacion.")
                }
                persistState(
                    definition = definition,
                    descriptor = existingDescriptor,
                    status = STATUS_NOT_INSTALLED,
                    lastError = "${spec.displayName} necesita reinstalacion manual desde Ajustes para actualizarse.",
                    downloadedBytes = 0L,
                    totalBytes = existing?.totalBytes ?: existingDescriptor?.sizeBytes ?: 0L
                )
                error("${spec.displayName} necesita reinstalacion manual.")
            }

            if (modelFile.exists()) {
                runCatching {
                    verifyIntegrity(spec, modelFile, existing?.expectedSha256 ?: existingDescriptor?.sha256)
                }.onSuccess {
                    persistState(
                        definition = definition,
                        descriptor = existingDescriptor,
                        status = STATUS_READY,
                        lastError = null,
                        downloadedBytes = modelFile.length(),
                        totalBytes = existing?.totalBytes ?: modelFile.length()
                    )
                    return@withLock
                }.onFailure {
                    modelFile.delete()
                    if (definition.spec.downloadPolicy == LocalModelDownloadPolicy.AUTOMATIC) {
                        requestModelDownload(spec.type)
                        persistState(
                            definition = definition,
                            descriptor = existingDescriptor,
                            status = STATUS_ERROR,
                            lastError = "El modelo ${spec.displayName} estaba corrupto y se ha solicitado una reinstalacion.",
                            downloadedBytes = 0L,
                            totalBytes = existing?.totalBytes ?: 0L
                        )
                        error("Integridad invalida para ${spec.displayName}. Se ha iniciado una reinstalacion.")
                    }
                    persistState(
                        definition = definition,
                        descriptor = existingDescriptor,
                        status = STATUS_ERROR,
                        lastError = "${spec.displayName} estaba corrupto. Borralo o reinstalalo manualmente desde Ajustes.",
                        downloadedBytes = 0L,
                        totalBytes = existing?.totalBytes ?: 0L
                    )
                    error("Integridad invalida para ${spec.displayName}. Reinstalacion manual requerida.")
                }
            }

            if (existing?.status in setOf(STATUS_DOWNLOADING, STATUS_VERIFYING, STATUS_INSTALLING, STATUS_INITIALIZING)) {
                error(existing?.toRuntimeMessage() ?: "${spec.displayName} se esta preparando todavia.")
            }

            if (definition.spec.downloadPolicy == LocalModelDownloadPolicy.EXPLICIT_USER_ACTION) {
                val message = "${spec.displayName} requiere descarga manual y consentimiento explicito desde Ajustes."
                persistState(
                    definition = definition,
                    descriptor = existingDescriptor,
                    status = STATUS_NOT_INSTALLED,
                    lastError = message,
                    downloadedBytes = existing?.downloadedBytes ?: 0L,
                    totalBytes = existing?.totalBytes ?: 0L
                )
                error(message)
            }
            requestModelDownload(spec.type).getOrElse { cause ->
                val message = cause.message ?: "No se pudo preparar ${spec.displayName}."
                persistState(
                    definition = definition,
                    descriptor = existingDescriptor,
                    status = STATUS_ERROR,
                    lastError = message,
                    downloadedBytes = existing?.downloadedBytes ?: 0L,
                    totalBytes = existing?.totalBytes ?: 0L
                )
                error(message)
            }
            error("${spec.displayName} no estaba instalado. Se ha programado la descarga en segundo plano.")
        }
    }

    override suspend fun releaseModels() {
        // Los providers gestionan sus caches internas cuando reevalÃºan el fichero local.
    }

    override suspend fun markModelError(type: LocalAiModelType, message: String) {
        val definition = LocalAiModelCatalog.definitionFor(type)
        val previous = modelStateDao.getByModelType(type.name)
        modelStateDao.upsert(
            LocalAiModelStateEntity(
                modelType = type.name,
                modelId = previous?.modelId ?: definition.spec.id,
                displayName = previous?.displayName ?: definition.spec.displayName,
                modelVersion = previous?.modelVersion ?: definition.spec.version,
                status = STATUS_ERROR,
                localPath = previous?.localPath ?: definition.spec.localRelativePath,
                requiredDiskBytes = previous?.requiredDiskBytes ?: definition.spec.requiredDiskBytes,
                requiredRamMb = previous?.requiredRamMb ?: definition.spec.requiredRamMb,
                supportedAbisCsv = previous?.supportedAbisCsv.orEmpty(),
                minSdk = previous?.minSdk ?: Build.VERSION.SDK_INT,
                downloadUrl = previous?.downloadUrl,
                expectedSha256 = previous?.expectedSha256 ?: definition.spec.expectedSha256,
                noticeUrl = previous?.noticeUrl ?: definition.spec.defaultNoticeUrl,
                termsUrl = previous?.termsUrl ?: definition.spec.defaultTermsUrl,
                prohibitedUsePolicyUrl = previous?.prohibitedUsePolicyUrl ?: definition.spec.defaultProhibitedUsePolicyUrl,
                downloadedBytes = previous?.downloadedBytes ?: 0L,
                totalBytes = previous?.totalBytes ?: 0L,
                lastError = message,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun evaluateDeviceCompatibility(
        spec: LocalAiModelSpec,
        descriptor: DownloadableLocalModelDescriptor?
    ): DeviceCompatibilityReport {
        val statFs = StatFs(context.filesDir.absolutePath)
        val availableDisk = statFs.availableBytes
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val availableRamMb = activityManager.memoryClass
        val requiredDiskBytes = descriptor?.requiredDiskBytes ?: spec.requiredDiskBytes
        val requiredRamMb = descriptor?.requiredRamMb ?: spec.requiredRamMb
        val supportedAbis = descriptor?.supportedAbis.orEmpty()
        val minSdk = descriptor?.minSdk ?: Build.VERSION.SDK_INT

        val blockers = buildList {
            if (supportedAbis.isNotEmpty() && supportedAbis.none { abi -> abi in Build.SUPPORTED_ABIS }) {
                add("ABI no compatible: ${spec.displayName} solo soporta ${supportedAbis.joinToString()}.")
            }
            if (Build.VERSION.SDK_INT < minSdk) {
                add("API no compatible: ${spec.displayName} requiere minSdk $minSdk.")
            }
            if (availableDisk < requiredDiskBytes) {
                add("Espacio insuficiente para ${spec.displayName}. Requiere $requiredDiskBytes bytes.")
            }
            if (availableRamMb < requiredRamMb) {
                add("RAM insuficiente para ${spec.displayName}. Requiere $requiredRamMb MB.")
            }
        }
        return DeviceCompatibilityReport(
            isSupported = blockers.isEmpty(),
            availableDiskBytes = availableDisk,
            availableRamMb = availableRamMb,
            blockers = blockers
        )
    }

    private suspend fun verifyIntegrity(spec: LocalAiModelSpec, modelFile: File, expectedSha256: String?) {
        if (!modelFile.exists()) {
            error("No existe el archivo de ${spec.displayName} en ${modelFile.absolutePath}")
        }
        if (expectedSha256.isNullOrBlank()) return
        val actual = sha256(modelFile)
        if (!actual.equals(expectedSha256, ignoreCase = true)) {
            error("SHA-256 invalido para ${spec.displayName}. Esperado=$expectedSha256 actual=$actual")
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

    private suspend fun persistState(
        definition: LocalAiModelDefinition,
        descriptor: DownloadableLocalModelDescriptor?,
        status: String,
        lastError: String?,
        downloadedBytes: Long,
        totalBytes: Long
    ) {
        val previous = modelStateDao.getByModelType(definition.spec.type.name)
        modelStateDao.upsert(
            LocalAiModelStateEntity(
                modelType = definition.spec.type.name,
                modelId = descriptor?.id ?: previous?.modelId ?: definition.spec.id,
                displayName = definition.spec.displayName,
                modelVersion = descriptor?.version ?: previous?.modelVersion ?: definition.spec.version,
                status = status,
                localPath = definition.spec.localRelativePath,
                requiredDiskBytes = descriptor?.requiredDiskBytes ?: previous?.requiredDiskBytes ?: definition.spec.requiredDiskBytes,
                requiredRamMb = descriptor?.requiredRamMb ?: previous?.requiredRamMb ?: definition.spec.requiredRamMb,
                supportedAbisCsv = descriptor?.supportedAbis?.joinToString(",") ?: previous?.supportedAbisCsv.orEmpty(),
                minSdk = descriptor?.minSdk ?: previous?.minSdk ?: Build.VERSION.SDK_INT,
                downloadUrl = descriptor?.downloadUrl ?: previous?.downloadUrl,
                expectedSha256 = descriptor?.sha256 ?: previous?.expectedSha256 ?: definition.spec.expectedSha256,
                noticeUrl = descriptor?.noticeUrl ?: previous?.noticeUrl ?: definition.spec.defaultNoticeUrl,
                termsUrl = descriptor?.termsUrl ?: previous?.termsUrl ?: definition.spec.defaultTermsUrl,
                prohibitedUsePolicyUrl = descriptor?.prohibitedUsePolicyUrl
                    ?: previous?.prohibitedUsePolicyUrl
                    ?: definition.spec.defaultProhibitedUsePolicyUrl,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                lastError = lastError,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

private fun LocalAiModelStateEntity.toRuntimeMessage(): String? = when (status) {
    STATUS_NOT_INSTALLED,
    STATUS_MISSING_MODEL -> lastError ?: "Modelo local no instalado todavia."
    STATUS_DOWNLOADING -> "Descargando modelo local ($displayName)..."
    STATUS_VERIFYING -> "Verificando integridad de $displayName..."
    STATUS_INSTALLING,
    STATUS_INITIALIZING -> "Instalando $displayName en almacenamiento privado..."
    STATUS_INCOMPATIBLE -> lastError ?: "Dispositivo no compatible con el modelo local"
    STATUS_NO_SPACE -> lastError ?: "Espacio insuficiente para instalar el modelo local"
    STATUS_INTEGRITY_ERROR -> lastError ?: "La verificaciÃ³n de integridad del modelo ha fallado"
    STATUS_ERROR -> lastError
    else -> lastError
}

private fun LocalAiModelStateEntity.toDescriptor(): DownloadableLocalModelDescriptor? {
    val url = downloadUrl?.takeIf { it.isNotBlank() } ?: return null
    val sha = expectedSha256?.takeIf { it.isNotBlank() } ?: return null
    val supportedAbis = supportedAbisCsv
        .split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return DownloadableLocalModelDescriptor(
        id = modelId,
        version = modelVersion,
        downloadUrl = url,
        sha256 = sha,
        sizeBytes = totalBytes,
        requiredRamMb = requiredRamMb,
        requiredDiskBytes = requiredDiskBytes,
        supportedAbis = supportedAbis,
        minSdk = minSdk,
        noticeUrl = noticeUrl,
        termsUrl = termsUrl,
        prohibitedUsePolicyUrl = prohibitedUsePolicyUrl
    )
}
