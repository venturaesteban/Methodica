package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.methodica.app.data.local.entity.TopicComplexityAnalysisEntity

@Dao
interface TopicComplexityAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TopicComplexityAnalysisEntity>)

    @Query("DELETE FROM topic_complexity_analysis WHERE analysisId = :analysisId")
    suspend fun deleteByAnalysisId(analysisId: Long)

    @Query("SELECT * FROM topic_complexity_analysis WHERE analysisId = :analysisId ORDER BY priority DESC")
    suspend fun getByAnalysisId(analysisId: Long): List<TopicComplexityAnalysisEntity>
}
