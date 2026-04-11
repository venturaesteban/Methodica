package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.methodica.app.data.local.entity.LocalAiModelStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalAiModelStateDao {

    @Query("SELECT * FROM local_ai_model_state")
    fun observeAll(): Flow<List<LocalAiModelStateEntity>>

    @Query("SELECT * FROM local_ai_model_state WHERE modelType = :modelType LIMIT 1")
    suspend fun getByModelType(modelType: String): LocalAiModelStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: LocalAiModelStateEntity)
}
