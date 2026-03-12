package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.methodica.app.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SubjectEntity?

    /** Inserta o actualiza una materia. @Upsert disponible desde Room 2.5 */
    @Upsert
    suspend fun upsert(subject: SubjectEntity)

    @Delete
    suspend fun delete(subject: SubjectEntity)
}
