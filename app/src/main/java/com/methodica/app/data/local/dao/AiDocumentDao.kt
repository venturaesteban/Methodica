package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.methodica.app.data.local.entity.AiDocumentEntity

@Dao
interface AiDocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: AiDocumentEntity): Long

    @Query("SELECT * FROM ai_documents WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AiDocumentEntity?
}
