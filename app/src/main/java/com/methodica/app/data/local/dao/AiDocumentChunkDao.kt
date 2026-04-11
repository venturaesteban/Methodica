package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.methodica.app.data.local.entity.AiDocumentChunkEntity

@Dao
interface AiDocumentChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chunks: List<AiDocumentChunkEntity>)

    @Query("DELETE FROM ai_document_chunks WHERE subjectId = :subjectId AND (:assessmentId IS NULL OR assessmentId = :assessmentId)")
    suspend fun deleteByContext(subjectId: Long, assessmentId: Long?)
}
