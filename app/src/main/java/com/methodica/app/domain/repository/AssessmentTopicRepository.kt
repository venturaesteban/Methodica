package com.methodica.app.domain.repository

import com.methodica.app.domain.model.Topic
import kotlinx.coroutines.flow.Flow

interface AssessmentTopicRepository {
    fun observeTopicsForAssessment(assessmentId: Long): Flow<List<Topic>>
    suspend fun getTopicsForAssessment(assessmentId: Long): List<Topic>
    fun observeTopicIdsForAssessment(assessmentId: Long): Flow<List<Long>>
    suspend fun replaceTopicsForAssessment(assessmentId: Long, topicIds: List<Long>)
}
