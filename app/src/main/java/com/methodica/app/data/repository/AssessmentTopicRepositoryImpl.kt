package com.methodica.app.data.repository

import com.methodica.app.data.local.dao.AssessmentTopicDao
import com.methodica.app.data.local.entity.toDomain
import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.repository.AssessmentTopicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AssessmentTopicRepositoryImpl(
    private val dao: AssessmentTopicDao
) : AssessmentTopicRepository {

    override fun observeTopicsForAssessment(assessmentId: Long): Flow<List<Topic>> =
        dao.observeTopicsForAssessment(assessmentId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTopicsForAssessment(assessmentId: Long): List<Topic> =
        dao.getTopicsForAssessment(assessmentId).map { it.toDomain() }

    override fun observeTopicIdsForAssessment(assessmentId: Long): Flow<List<Long>> =
        dao.observeTopicIdsForAssessment(assessmentId)

    override suspend fun replaceTopicsForAssessment(assessmentId: Long, topicIds: List<Long>) =
        dao.replaceTopicsForAssessment(assessmentId, topicIds)
}
