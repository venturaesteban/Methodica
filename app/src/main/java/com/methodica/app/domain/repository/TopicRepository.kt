package com.methodica.app.domain.repository

import com.methodica.app.domain.model.Topic
import kotlinx.coroutines.flow.Flow

interface TopicRepository {
    fun observeTopics(subjectId: Long): Flow<List<Topic>>
    suspend fun getTopic(id: Long): Topic?
    suspend fun saveTopic(topic: Topic)
    suspend fun deleteTopic(topic: Topic)
}
