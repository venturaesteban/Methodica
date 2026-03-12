package com.methodica.app.domain.usecase.topic

import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.repository.TopicRepository

class GetTopicUseCase(private val repository: TopicRepository) {
    suspend operator fun invoke(id: Long): Topic? = repository.getTopic(id)
}
