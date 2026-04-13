package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_ai_model_state")
data class LocalAiModelStateEntity(
    @PrimaryKey val modelType: String,
    val modelId: String,
    val displayName: String,
    val modelVersion: String,
    val status: String,
    val localPath: String,
    val requiredDiskBytes: Long,
    val requiredRamMb: Int,
    val supportedAbisCsv: String,
    val minSdk: Int,
    val downloadUrl: String?,
    val expectedSha256: String?,
    val noticeUrl: String?,
    val termsUrl: String?,
    val prohibitedUsePolicyUrl: String?,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val lastError: String?,
    val updatedAt: Long
)
