package com.methodica.app.data.localai.runtime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.methodica.app.R
import com.methodica.app.data.local.AppDatabase
import com.methodica.app.data.local.AppDatabaseMigrations
import com.methodica.app.data.local.dao.LocalAiModelStateDao
import com.methodica.app.data.local.entity.LocalAiModelStateEntity
import com.methodica.app.domain.ai.local.DownloadableLocalModelDescriptor
import com.methodica.app.domain.ai.local.LocalAiModelType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.CancellationException

class LocalModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val modelType = inputData.getString(KEY_MODEL_TYPE)
            ?.let { value -> runCatching { LocalAiModelType.valueOf(value) }.getOrNull() }
            ?: return Result.failure()

        val definition = LocalAiModelCatalog.definitionFor(modelType)
        val resolver = LocalModelDistributionResolver(applicationContext)
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "methodica.db"
        )
            .addMigrations(*AppDatabaseMigrations.ALL)
            .build()

        try {
            val modelStateDao = database.localAiModelStateDao()
            val resolved = resolver.resolve(modelType)
            val descriptor = resolved.descriptor
            if (descriptor == null) {
                persistState(
                    modelStateDao = modelStateDao,
                    definition = definition,
                    descriptor = null,
                    status = STATUS_ERROR,
                    lastError = resolved.resolutionError ?: "No hay distribucion remota disponible para ${definition.spec.displayName}.",
                    downloadedBytes = 0L,
                    totalBytes = 0L
                )
                return Result.failure()
            }

            val compatibility = evaluateCompatibility(descriptor)
            if (!compatibility.isSupported) {
                val status = if (compatibility.reason.contains("Espacio", ignoreCase = true)) STATUS_NO_SPACE else STATUS_INCOMPATIBLE
                persistState(
                    modelStateDao = modelStateDao,
                    definition = definition,
                    descriptor = descriptor,
                    status = status,
                    lastError = compatibility.reason,
                    downloadedBytes = 0L,
                    totalBytes = descriptor.sizeBytes
                )
                return Result.failure()
            }

            setForeground(createForegroundInfo(definition.spec.displayName, "Preparando descarga"))

            val destination = applicationContext.filesDir.resolve(definition.spec.localRelativePath)
            destination.parentFile?.mkdirs()
            val tempFile = File(destination.parentFile, "${destination.name}.download")
            tempFile.delete()

            downloadToTempFile(
                definition = definition,
                descriptor = descriptor,
                tempFile = tempFile,
                modelStateDao = modelStateDao
            )

            persistState(
                modelStateDao = modelStateDao,
                definition = definition,
                descriptor = descriptor,
                status = STATUS_VERIFYING,
                lastError = null,
                downloadedBytes = tempFile.length(),
                totalBytes = descriptor.sizeBytes
            )
            verifyDownloadedFile(definition.spec, descriptor, tempFile)

            persistState(
                modelStateDao = modelStateDao,
                definition = definition,
                descriptor = descriptor,
                status = STATUS_INSTALLING,
                lastError = null,
                downloadedBytes = tempFile.length(),
                totalBytes = descriptor.sizeBytes
            )
            moveAtomically(tempFile, destination)
            cleanupObsoleteFiles(destination)

            persistState(
                modelStateDao = modelStateDao,
                definition = definition,
                descriptor = descriptor,
                status = STATUS_READY,
                lastError = null,
                downloadedBytes = descriptor.sizeBytes,
                totalBytes = descriptor.sizeBytes
            )
            return Result.success()
        } catch (_: CancellationException) {
            persistCancellation(modelType)
            return Result.failure()
        } catch (error: Throwable) {
            persistFailure(modelType, error)
            return Result.failure()
        } finally {
            database.close()
        }
    }

    private suspend fun downloadToTempFile(
        definition: LocalAiModelDefinition,
        descriptor: DownloadableLocalModelDescriptor,
        tempFile: File,
        modelStateDao: LocalAiModelStateDao
    ) {
        val connection = (URL(descriptor.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            requestMethod = "GET"
        }
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            error("Descarga fallida para ${definition.spec.displayName}: HTTP $responseCode")
        }

        val announcedTotalBytes = connection.contentLengthLong.takeIf { it > 0L } ?: descriptor.sizeBytes
        persistState(
            modelStateDao = modelStateDao,
            definition = definition,
            descriptor = descriptor,
            status = STATUS_DOWNLOADING,
            lastError = null,
            downloadedBytes = 0L,
            totalBytes = announcedTotalBytes
        )

        connection.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloadedBytes = 0L
                var lastPersistedBytes = 0L
                while (true) {
                    ensureNotStopped()
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    if (downloadedBytes - lastPersistedBytes >= PROGRESS_PERSIST_STEP_BYTES || downloadedBytes == announcedTotalBytes) {
                        lastPersistedBytes = downloadedBytes
                        persistState(
                            modelStateDao = modelStateDao,
                            definition = definition,
                            descriptor = descriptor,
                            status = STATUS_DOWNLOADING,
                            lastError = null,
                            downloadedBytes = downloadedBytes,
                            totalBytes = announcedTotalBytes
                        )
                        setForeground(
                            createForegroundInfo(
                                definition.spec.displayName,
                                buildString {
                                    append("Descargando ")
                                    append((downloadedBytes / 1024L / 1024L).coerceAtLeast(0L))
                                    append(" MB")
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    private fun evaluateCompatibility(descriptor: DownloadableLocalModelDescriptor): CompatibilityCheck {
        val supportedAbis = Build.SUPPORTED_ABIS.toSet()
        val abiSupported = descriptor.supportedAbis.isEmpty() || descriptor.supportedAbis.any { it in supportedAbis }
        if (!abiSupported) {
            return CompatibilityCheck(false, "ABI no compatible para ${descriptor.id}. Soportadas: ${descriptor.supportedAbis.joinToString()}")
        }
        if (Build.VERSION.SDK_INT < descriptor.minSdk) {
            return CompatibilityCheck(false, "API ${Build.VERSION.SDK_INT} no compatible. ${descriptor.id} requiere minSdk ${descriptor.minSdk}.")
        }
        val availableDiskBytes = StatFs(applicationContext.filesDir.absolutePath).availableBytes
        if (availableDiskBytes < descriptor.requiredDiskBytes) {
            return CompatibilityCheck(false, "Espacio insuficiente para ${descriptor.id}. Requiere ${descriptor.requiredDiskBytes} bytes.")
        }
        return CompatibilityCheck(true, "")
    }

    private fun verifyDownloadedFile(
        spec: com.methodica.app.domain.ai.local.LocalAiModelSpec,
        descriptor: DownloadableLocalModelDescriptor,
        tempFile: File
    ) {
        if (!tempFile.exists()) {
            error("El archivo temporal de ${spec.displayName} no existe.")
        }
        val actualSize = tempFile.length()
        if (actualSize <= 0L) {
            error("La descarga de ${spec.displayName} ha quedado vacia.")
        }
        if (descriptor.sizeBytes > 0L && actualSize != descriptor.sizeBytes) {
            error("Tamano inesperado para ${spec.displayName}. Esperado=${descriptor.sizeBytes} actual=$actualSize")
        }
        val actualSha = sha256(tempFile)
        if (!actualSha.equals(descriptor.sha256, ignoreCase = true)) {
            error("SHA-256 invalido para ${spec.displayName}.")
        }
    }

    private fun moveAtomically(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun cleanupObsoleteFiles(currentFile: File) {
        currentFile.parentFile?.listFiles()?.forEach { candidate ->
            if (candidate.absolutePath == currentFile.absolutePath) return@forEach
            if (candidate.name.endsWith(".download", ignoreCase = true)) {
                candidate.delete()
                return@forEach
            }
            if (candidate.isFile) {
                candidate.delete()
            }
        }
    }

    private suspend fun persistCancellation(modelType: LocalAiModelType) {
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "methodica.db"
        )
            .addMigrations(*AppDatabaseMigrations.ALL)
            .build()
        try {
            val definition = LocalAiModelCatalog.definitionFor(modelType)
            val previous = database.localAiModelStateDao().getByModelType(modelType.name)
            database.localAiModelStateDao().upsert(
                LocalAiModelStateEntity(
                    modelType = modelType.name,
                    modelId = previous?.modelId ?: definition.spec.id,
                    displayName = previous?.displayName ?: definition.spec.displayName,
                    modelVersion = previous?.modelVersion ?: definition.spec.version,
                    status = STATUS_NOT_INSTALLED,
                    localPath = previous?.localPath ?: definition.spec.localRelativePath,
                    requiredDiskBytes = previous?.requiredDiskBytes ?: definition.spec.requiredDiskBytes,
                    requiredRamMb = previous?.requiredRamMb ?: definition.spec.requiredRamMb,
                    supportedAbisCsv = previous?.supportedAbisCsv.orEmpty(),
                    minSdk = previous?.minSdk ?: 26,
                    downloadUrl = previous?.downloadUrl,
                    expectedSha256 = previous?.expectedSha256 ?: definition.spec.expectedSha256,
                    downloadedBytes = 0L,
                    totalBytes = previous?.totalBytes ?: 0L,
                    lastError = "Descarga cancelada por el usuario.",
                    updatedAt = System.currentTimeMillis()
                )
            )
        } finally {
            database.close()
        }
    }

    private suspend fun persistFailure(modelType: LocalAiModelType, error: Throwable) {
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "methodica.db"
        )
            .addMigrations(*AppDatabaseMigrations.ALL)
            .build()
        try {
            val definition = LocalAiModelCatalog.definitionFor(modelType)
            val previous = database.localAiModelStateDao().getByModelType(modelType.name)
            val status = if (error.isNoSpaceError()) STATUS_NO_SPACE else STATUS_ERROR
            database.localAiModelStateDao().upsert(
                LocalAiModelStateEntity(
                    modelType = modelType.name,
                    modelId = previous?.modelId ?: definition.spec.id,
                    displayName = previous?.displayName ?: definition.spec.displayName,
                    modelVersion = previous?.modelVersion ?: definition.spec.version,
                    status = status,
                    localPath = previous?.localPath ?: definition.spec.localRelativePath,
                    requiredDiskBytes = previous?.requiredDiskBytes ?: definition.spec.requiredDiskBytes,
                    requiredRamMb = previous?.requiredRamMb ?: definition.spec.requiredRamMb,
                    supportedAbisCsv = previous?.supportedAbisCsv.orEmpty(),
                    minSdk = previous?.minSdk ?: 26,
                    downloadUrl = previous?.downloadUrl,
                    expectedSha256 = previous?.expectedSha256 ?: definition.spec.expectedSha256,
                    downloadedBytes = previous?.downloadedBytes ?: 0L,
                    totalBytes = previous?.totalBytes ?: 0L,
                    lastError = error.message ?: "Fallo no clasificado descargando ${definition.spec.displayName}.",
                    updatedAt = System.currentTimeMillis()
                )
            )
        } finally {
            database.close()
        }
    }

    private suspend fun persistState(
        modelStateDao: LocalAiModelStateDao,
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
                requiredDiskBytes = descriptor?.requiredDiskBytes ?: definition.spec.requiredDiskBytes,
                requiredRamMb = descriptor?.requiredRamMb ?: definition.spec.requiredRamMb,
                supportedAbisCsv = descriptor?.supportedAbis?.joinToString(",").orEmpty(),
                minSdk = descriptor?.minSdk ?: previous?.minSdk ?: Build.VERSION.SDK_INT,
                downloadUrl = descriptor?.downloadUrl ?: previous?.downloadUrl,
                expectedSha256 = descriptor?.sha256 ?: definition.spec.expectedSha256,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                lastError = lastError,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun ensureNotStopped() {
        if (isStopped) {
            throw CancellationException("La descarga fue cancelada.")
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

    private fun Throwable.isNoSpaceError(): Boolean {
        val message = message.orEmpty()
        return this is IOException &&
            (message.contains("No space left on device", ignoreCase = true) ||
                message.contains("ENOSPC", ignoreCase = true))
    }

    private fun createForegroundInfo(modelName: String, contentText: String): ForegroundInfo {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Methodica")
            .setContentText("$modelName: $contentText")
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Descarga de modelos locales",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_MODEL_TYPE = "model_type"
        private const val NOTIFICATION_CHANNEL_ID = "methodica_local_model_downloads"
        private const val NOTIFICATION_ID = 8101
        private const val PROGRESS_PERSIST_STEP_BYTES = 2L * 1024L * 1024L
    }
}

private data class CompatibilityCheck(
    val isSupported: Boolean,
    val reason: String
)
