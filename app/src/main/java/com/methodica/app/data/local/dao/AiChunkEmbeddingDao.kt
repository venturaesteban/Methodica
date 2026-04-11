package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.methodica.app.data.local.entity.AiChunkEmbeddingEntity

data class EmbeddingChunkRow(
    val externalId: String,
    val subjectId: Long,
    val assessmentId: Long?,
    val topicId: Long?,
    val materialId: Long?,
    val documentId: Long?,
    val content: String,
    val vector: String,
    val dimensions: Int
)

@Dao
interface AiChunkEmbeddingDao {

    @Upsert
    suspend fun upsert(embeddings: List<AiChunkEmbeddingEntity>)

    @Query("DELETE FROM ai_chunk_embeddings WHERE chunkId IN (:chunkIds)")
    suspend fun deleteByChunkIds(chunkIds: List<Long>)

    @Query(
        """
        SELECT c.externalId as externalId,
               c.subjectId as subjectId,
               c.assessmentId as assessmentId,
               c.topicId as topicId,
               c.materialId as materialId,
               c.documentId as documentId,
               c.content as content,
               e.vector as vector,
               e.dimensions as dimensions
          FROM ai_chunk_embeddings e
          INNER JOIN ai_document_chunks c ON c.id = e.chunkId
         WHERE c.subjectId = :subjectId
           AND (:assessmentId IS NULL OR c.assessmentId = :assessmentId)
           AND (:topicId IS NULL OR c.topicId = :topicId)
           AND (:materialId IS NULL OR c.materialId = :materialId)
           AND (:documentId IS NULL OR c.documentId = :documentId)
        """
    )
    suspend fun getIndexedRows(
        subjectId: Long,
        assessmentId: Long?,
        topicId: Long?,
        materialId: Long?,
        documentId: Long?
    ): List<EmbeddingChunkRow>
}
