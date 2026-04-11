package com.methodica.app.domain.repository

import com.methodica.app.domain.model.Material
import kotlinx.coroutines.flow.Flow

interface MaterialRepository {
    fun observeAllMaterials(): Flow<List<Material>>
    suspend fun getMaterial(id: Long): Material?
    suspend fun saveMaterial(material: Material): Long
    suspend fun deleteMaterial(material: Material)
    suspend fun buildAiResourceSummary(material: Material, maxChars: Int = 2500): String?
    suspend fun buildAiIndexableContent(material: Material, maxChars: Int = 24000): String?
}
