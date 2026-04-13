package com.methodica.app.data.localai.runtime

import android.content.Context
import com.methodica.app.domain.ai.local.DownloadableLocalModelDescriptor
import com.methodica.app.domain.ai.local.DownloadableLocalModelManifest
import com.methodica.app.domain.ai.local.LocalAiModelType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ResolvedModelDistribution(
    val definition: LocalAiModelDefinition,
    val descriptor: DownloadableLocalModelDescriptor?,
    val resolutionError: String?,
    val manifestConfigured: Boolean
)

@Singleton
class LocalModelDistributionResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val manifestConfig: LocalModelManifestConfig,
    private val manifestParser: LocalModelManifestParser
) {

    suspend fun resolveAll(): List<ResolvedModelDistribution> {
        val manifestResult = fetchRemoteManifest()
        val manifest = manifestResult.getOrNull()
        val manifestConfigured = configuredManifestUrl().isNotBlank()
        val fetchError = manifestResult.exceptionOrNull()?.message

        return LocalAiModelCatalog.allDefinitions.map { definition ->
            resolveDefinition(
                definition = definition,
                manifest = manifest,
                manifestConfigured = manifestConfigured,
                fetchError = fetchError
            )
        }
    }

    suspend fun resolve(type: LocalAiModelType): ResolvedModelDistribution {
        val manifestResult = fetchRemoteManifest()
        val manifest = manifestResult.getOrNull()
        val manifestConfigured = configuredManifestUrl().isNotBlank()
        val fetchError = manifestResult.exceptionOrNull()?.message
        return resolveDefinition(
            definition = LocalAiModelCatalog.definitionFor(type),
            manifest = manifest,
            manifestConfigured = manifestConfigured,
            fetchError = fetchError
        )
    }

    private suspend fun fetchRemoteManifest(): Result<DownloadableLocalModelManifest?> = withContext(Dispatchers.IO) {
        val manifestUrl = configuredManifestUrl()
        if (manifestUrl.isBlank()) {
            return@withContext Result.success(null)
        }

        runCatching {
            val connection = URL(manifestUrl).openConnection()
            if (connection is HttpURLConnection) {
                connection.connectTimeout = 20_000
                connection.readTimeout = 20_000
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    error("Manifest remoto devolvio HTTP $responseCode")
                }
            }

            connection.getInputStream().bufferedReader().use { reader ->
                manifestParser.parse(
                    rawJson = reader.readText(),
                    defaultMinSdk = context.applicationInfo.minSdkVersion
                )
            }
        }
    }

    private fun configuredManifestUrl(): String = manifestConfig.configuredManifestUrl()

    private fun resolveDefinition(
        definition: LocalAiModelDefinition,
        manifest: DownloadableLocalModelManifest?,
        manifestConfigured: Boolean,
        fetchError: String?
    ): ResolvedModelDistribution {
        val spec = definition.spec
        val remoteModels = manifest?.models.orEmpty()
        val exactMatch = remoteModels.firstOrNull { it.id == spec.id && it.version == spec.version }
        if (exactMatch != null) {
            val validationError = validateDescriptor(spec, exactMatch)
            return ResolvedModelDistribution(
                definition = definition,
                descriptor = exactMatch.takeIf { validationError == null },
                resolutionError = validationError,
                manifestConfigured = manifestConfigured
            )
        }

        val idMatch = remoteModels.firstOrNull { it.id == spec.id }
        if (idMatch != null) {
            return ResolvedModelDistribution(
                definition = definition,
                descriptor = null,
                resolutionError = "El manifest remoto publica ${spec.displayName} en ${idMatch.version}, pero esta build espera ${spec.version}.",
                manifestConfigured = manifestConfigured
            )
        }

        definition.fallbackDownload?.let { fallback ->
            val validationError = validateDescriptor(spec, fallback)
            return ResolvedModelDistribution(
                definition = definition,
                descriptor = fallback.takeIf { validationError == null },
                resolutionError = validationError,
                manifestConfigured = manifestConfigured
            )
        }

        val error = when {
            fetchError != null -> "No se pudo obtener el manifest remoto para ${spec.displayName}: $fetchError"
            manifestConfigured -> "${spec.displayName} no esta publicado en el manifest remoto configurado."
            spec.redistributionRequiresLicenseConfirmation ->
                "La distribucion remota de ${spec.displayName} sigue pendiente de confirmar licencia y publicar su manifest."
            else -> "No hay distribucion remota configurada para ${spec.displayName}."
        }

        return ResolvedModelDistribution(
            definition = definition,
            descriptor = null,
            resolutionError = error,
            manifestConfigured = manifestConfigured
        )
    }

    private fun validateDescriptor(spec: com.methodica.app.domain.ai.local.LocalAiModelSpec, descriptor: DownloadableLocalModelDescriptor): String? {
        if (descriptor.version != spec.version) {
            return "Version de distribucion incompatible para ${spec.displayName}: ${descriptor.version}"
        }
        val expectedSha = spec.expectedSha256
        if (!expectedSha.isNullOrBlank() && !descriptor.sha256.equals(expectedSha, ignoreCase = true)) {
            return "SHA-256 remoto incompatible para ${spec.displayName}."
        }
        if (descriptor.downloadUrl.isBlank()) {
            return "La URL de descarga de ${spec.displayName} esta vacia."
        }
        return null
    }
}
