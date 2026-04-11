package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.methodica.app.data.local.entity.AiAnalysisEntity

@Dao
interface AiAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: AiAnalysisEntity): Long

    @Query("SELECT * FROM ai_analysis WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AiAnalysisEntity?

    @Query("SELECT * FROM ai_analysis WHERE assessmentId = :assessmentId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByAssessmentId(assessmentId: Long): AiAnalysisEntity?
}
