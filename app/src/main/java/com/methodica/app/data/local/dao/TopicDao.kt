package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.methodica.app.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {

    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY `order` ASC")
    fun observeBySubjectId(subjectId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TopicEntity?

    @Upsert
    suspend fun upsert(topic: TopicEntity)

    @Delete
    suspend fun delete(topic: TopicEntity)
}
