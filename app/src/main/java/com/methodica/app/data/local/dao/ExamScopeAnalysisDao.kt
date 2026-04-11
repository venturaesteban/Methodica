package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.methodica.app.data.local.entity.ExamScopeAnalysisEntity

@Dao
interface ExamScopeAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scope: ExamScopeAnalysisEntity): Long

    @Update
    suspend fun update(scope: ExamScopeAnalysisEntity)

    @Query("SELECT * FROM exam_scope_analysis WHERE analysisId = :analysisId LIMIT 1")
    suspend fun getByAnalysisId(analysisId: Long): ExamScopeAnalysisEntity?
}
