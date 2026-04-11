package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_chunk_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = AiDocumentChunkEntity::class,
            parentColumns = ["id"],
            childColumns = ["chunkId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chunkId", unique = true), Index("updatedAt")]
)
data class AiChunkEmbeddingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chunkId: Long,
    val modelVersion: String,
    val vector: String,
    val dimensions: Int,
    val updatedAt: Long
)
