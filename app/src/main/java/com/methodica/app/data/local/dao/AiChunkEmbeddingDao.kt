package com.methodica.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.methodica.app.data.local.entity.AiChunkEmbeddingEntity

@Dao
interface AiChunkEmbeddingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(embeddings: List<AiChunkEmbeddingEntity>)

    @Query("DELETE FROM ai_chunk_embeddings WHERE chunkId IN (:chunkIds)")
    suspend fun deleteByChunkIds(chunkIds: List<Long>)
}
