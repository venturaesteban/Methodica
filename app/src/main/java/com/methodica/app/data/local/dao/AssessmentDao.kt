package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.methodica.app.data.local.entity.AssessmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {

    @Query("SELECT * FROM assessments ORDER BY date ASC")
    fun observeAll(): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE subjectId = :subjectId ORDER BY date ASC")
    fun observeBySubjectId(subjectId: Long): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AssessmentEntity?

    @Query("SELECT * FROM assessments WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getByDateRange(startDate: Long, endDate: Long): List<AssessmentEntity>

    @Upsert
    suspend fun upsert(assessment: AssessmentEntity): Long

    @Delete
    suspend fun delete(assessment: AssessmentEntity)
}
