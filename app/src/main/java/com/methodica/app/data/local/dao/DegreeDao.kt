package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.methodica.app.data.local.entity.DegreeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DegreeDao {

    @Query("SELECT * FROM degrees ORDER BY name ASC")
    fun observeAll(): Flow<List<DegreeEntity>>

    @Query("SELECT * FROM degrees WHERE status = :status ORDER BY name ASC")
    fun observeByStatus(status: String): Flow<List<DegreeEntity>>

    @Query("SELECT * FROM degrees WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DegreeEntity?

    @Query("SELECT * FROM degrees WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): DegreeEntity?

    @Upsert
    suspend fun upsert(degree: DegreeEntity): Long

    @Query("UPDATE degrees SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM degrees WHERE id = :id")
    suspend fun deleteById(id: Long)
}

