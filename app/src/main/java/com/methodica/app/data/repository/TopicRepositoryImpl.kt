package com.methodica.app.data.repository

import com.methodica.app.data.local.dao.TopicDao
import com.methodica.app.data.local.entity.toDomain
import com.methodica.app.data.local.entity.toEntity
import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TopicRepositoryImpl(
    private val dao: TopicDao
) : TopicRepository {

    override fun observeTopics(subjectId: Long): Flow<List<Topic>> =
        dao.observeBySubjectId(subjectId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTopic(id: Long): Topic? =
        dao.getById(id)?.toDomain()

    override suspend fun saveTopic(topic: Topic) =
        dao.upsert(topic.toEntity())

    override suspend fun deleteTopic(topic: Topic) =
        dao.delete(topic.toEntity())
}
