package com.methodica.app.data.localai.runtime

import com.methodica.app.data.local.entity.LocalAiModelStateEntity
import com.methodica.app.domain.ai.local.LocalAiModelType
import com.methodica.app.domain.ai.local.LocalModelInstallState
import com.methodica.app.domain.ai.local.LocalModelInstallStatus

internal const val STATUS_NOT_INSTALLED = "NOT_INSTALLED"
internal const val STATUS_READY = "READY"
internal const val STATUS_ERROR = "ERROR"
internal const val STATUS_DOWNLOADING = "DOWNLOADING"
internal const val STATUS_VERIFYING = "VERIFYING"
internal const val STATUS_INSTALLING = "INSTALLING"
internal const val STATUS_INITIALIZING = "INITIALIZING"
internal const val STATUS_MISSING_MODEL = "MISSING_MODEL"
internal const val STATUS_INCOMPATIBLE = "INCOMPATIBLE_DEVICE"
internal const val STATUS_NO_SPACE = "NO_SPACE"
internal const val STATUS_INTEGRITY_ERROR = "INTEGRITY_ERROR"

internal val TERMINAL_ERROR_STATUSES = setOf(
    STATUS_ERROR,
    STATUS_MISSING_MODEL,
    STATUS_INCOMPATIBLE,
    STATUS_NO_SPACE,
    STATUS_INTEGRITY_ERROR
)

internal fun LocalAiModelType.uniqueDownloadWorkName(): String = "methodica_local_model_${name.lowercase()}_download"

internal fun mapDbStatusToInstallStatus(status: String): LocalModelInstallStatus = when (status) {
    STATUS_READY -> LocalModelInstallStatus.READY
    STATUS_DOWNLOADING -> LocalModelInstallStatus.DOWNLOADING
    STATUS_VERIFYING -> LocalModelInstallStatus.VERIFYING
    STATUS_INSTALLING,
    STATUS_INITIALIZING -> LocalModelInstallStatus.INSTALLING
    STATUS_INCOMPATIBLE -> LocalModelInstallStatus.INCOMPATIBLE_DEVICE
    STATUS_NO_SPACE -> LocalModelInstallStatus.NO_SPACE
    else -> LocalModelInstallStatus.ERROR
}

internal fun LocalAiModelStateEntity.toInstallState(definition: LocalAiModelDefinition): LocalModelInstallState {
    val supportedAbis = supportedAbisCsv
        .split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .ifEmpty { definition.fallbackDownload?.supportedAbis.orEmpty() }

    return LocalModelInstallState(
        type = definition.spec.type,
        modelId = modelId.ifBlank { definition.spec.id },
        displayName = displayName.ifBlank { definition.spec.displayName },
        modelVersion = modelVersion.ifBlank { definition.spec.version },
        downloadPolicy = definition.spec.downloadPolicy,
        usageSummary = definition.spec.usageSummary,
        status = when (status) {
            STATUS_NOT_INSTALLED -> LocalModelInstallStatus.NOT_INSTALLED
            STATUS_MISSING_MODEL -> LocalModelInstallStatus.NOT_INSTALLED
            else -> mapDbStatusToInstallStatus(status)
        },
        localRelativePath = localPath.ifBlank { definition.spec.localRelativePath },
        requiredDiskBytes = if (requiredDiskBytes > 0L) requiredDiskBytes else definition.spec.requiredDiskBytes,
        requiredRamMb = if (requiredRamMb > 0) requiredRamMb else definition.spec.requiredRamMb,
        supportedAbis = supportedAbis,
        minSdk = if (minSdk > 0) minSdk else definition.fallbackDownload?.minSdk ?: 26,
        recommendedOnWifi = definition.spec.recommendedOnWifi,
        noticeUrl = noticeUrl ?: definition.fallbackDownload?.noticeUrl ?: definition.spec.defaultNoticeUrl,
        termsUrl = termsUrl ?: definition.fallbackDownload?.termsUrl ?: definition.spec.defaultTermsUrl,
        prohibitedUsePolicyUrl = prohibitedUsePolicyUrl
            ?: definition.fallbackDownload?.prohibitedUsePolicyUrl
            ?: definition.spec.defaultProhibitedUsePolicyUrl,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        lastError = lastError,
        updatedAt = updatedAt,
        isDownloadConfigured = !downloadUrl.isNullOrBlank() || definition.fallbackDownload != null,
        redistributionRequiresLicenseConfirmation = definition.spec.redistributionRequiresLicenseConfirmation
    )
}
