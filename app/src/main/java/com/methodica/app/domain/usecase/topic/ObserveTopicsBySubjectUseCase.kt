package com.methodica.app.domain.usecase.topic

import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow

class ObserveTopicsBySubjectUseCase(private val repository: TopicRepository) {
    operator fun invoke(subjectId: Long): Flow<List<Topic>> = repository.observeTopics(subjectId)
}
