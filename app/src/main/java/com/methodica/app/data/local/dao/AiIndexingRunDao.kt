package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.methodica.app.data.local.entity.AiIndexingRunEntity

@Dao
interface AiIndexingRunDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(run: AiIndexingRunEntity): Long

    @Query("UPDATE ai_indexing_runs SET status = :status, completedAt = :completedAt, errorMessage = :errorMessage WHERE id = :id")
    suspend fun finish(id: Long, status: String, completedAt: Long, errorMessage: String?)
}
