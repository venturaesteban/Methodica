package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.methodica.app.data.local.entity.AiDocumentChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiDocumentChunkDao {

    @Upsert
    suspend fun upsert(chunks: List<AiDocumentChunkEntity>)

    @Query("SELECT * FROM ai_document_chunks WHERE externalId IN (:externalIds)")
    suspend fun getByExternalIds(externalIds: List<String>): List<AiDocumentChunkEntity>

    @Query(
        """
        SELECT * FROM ai_document_chunks
        WHERE subjectId = :subjectId
          AND (:assessmentId IS NULL OR assessmentId = :assessmentId)
          AND (:topicId IS NULL OR topicId = :topicId)
          AND (:materialId IS NULL OR materialId = :materialId)
          AND (:documentId IS NULL OR documentId = :documentId)
        """
    )
    suspend fun getBySource(
        subjectId: Long,
        assessmentId: Long?,
        topicId: Long?,
        materialId: Long?,
        documentId: Long?
    ): List<AiDocumentChunkEntity>

    @Query("DELETE FROM ai_document_chunks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM ai_document_chunks WHERE subjectId = :subjectId AND (:assessmentId IS NULL OR assessmentId = :assessmentId)")
    suspend fun deleteByContext(subjectId: Long, assessmentId: Long?)

    @Query(
        """
        SELECT COUNT(1) FROM ai_document_chunks
        WHERE subjectId = :subjectId
          AND (:materialId IS NULL OR materialId = :materialId)
        """
    )
    fun observeIndexedChunkCount(subjectId: Long, materialId: Long?): Flow<Int>
}
