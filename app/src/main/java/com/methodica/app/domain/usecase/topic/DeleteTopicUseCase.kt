package com.methodica.app.domain.usecase.topic

import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.repository.TopicRepository

class DeleteTopicUseCase(private val repository: TopicRepository) {
    suspend operator fun invoke(topic: Topic) = repository.deleteTopic(topic)
}
