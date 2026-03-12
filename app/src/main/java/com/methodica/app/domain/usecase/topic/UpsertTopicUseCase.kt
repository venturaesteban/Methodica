package com.methodica.app.domain.usecase.topic

import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.repository.TopicRepository

class UpsertTopicUseCase(private val repository: TopicRepository) {

    suspend operator fun invoke(topic: Topic): Result<Unit> {
        if (topic.name.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre del tema no puede estar vacío"))
        }
        if (topic.difficulty !in 1..3) {
            return Result.failure(IllegalArgumentException("La dificultad debe estar entre 1 y 3"))
        }
        if (topic.estimatedHours <= 0) {
            return Result.failure(IllegalArgumentException("Las horas estimadas deben ser mayores que 0"))
        }
        if (topic.order < 0) {
            return Result.failure(IllegalArgumentException("El orden debe ser mayor o igual a 0"))
        }
        return runCatching { repository.saveTopic(topic) }
    }
}
