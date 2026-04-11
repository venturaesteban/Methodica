package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_ai_model_state")
data class LocalAiModelStateEntity(
    @PrimaryKey val modelType: String,
    val modelId: String,
    val modelVersion: String,
    val status: String,
    val localPath: String,
    val requiredDiskBytes: Long,
    val requiredRamMb: Int,
    val lastError: String?,
    val updatedAt: Long
)
