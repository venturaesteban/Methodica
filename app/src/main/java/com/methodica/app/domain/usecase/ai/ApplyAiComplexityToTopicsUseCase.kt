package com.methodica.app.domain.usecase.ai

import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.model.TopicComplexityAnalysis
import com.methodica.app.domain.repository.TopicRepository
import java.text.Normalizer

class ApplyAiComplexityToTopicsUseCase(
    private val topicRepository: TopicRepository
) {
    suspend operator fun invoke(
        originalTopics: List<Topic>,
        editedComplexities: List<TopicComplexityAnalysis>
    ): Result<Unit> = runCatching {
        val mapByName = editedComplexities.associateBy { normalize(it.topicName) }
        originalTopics.forEach { topic ->
            val normalizedTopic = normalize(topic.name)
            val direct = mapByName[normalizedTopic]
            val analysis = direct ?: findBestMatch(normalizedTopic, editedComplexities) ?: return@forEach
            if (!analysis.isIncludedInScope) return@forEach

            val mappedDifficulty = when (analysis.complexityLevel) {
                1, 2 -> 1
                3 -> 2
                else -> 3
            }
            topicRepository.saveTopic(
                topic.copy(
                    difficulty = mappedDifficulty,
                    estimatedHours = analysis.recommendedHours.coerceAtLeast(1)
                )
            )
        }
    }

    private fun findBestMatch(
        normalizedTopic: String,
        editedComplexities: List<TopicComplexityAnalysis>
    ): TopicComplexityAnalysis? {
        val candidates = editedComplexities.map { candidate ->
            val normalizedCandidate = normalize(candidate.topicName)
            val score = similarity(normalizedTopic, normalizedCandidate)
            candidate to score
        }
        val best = candidates.maxByOrNull { it.second } ?: return null
        return if (best.second >= 0.55f) best.first else null
    }

    private fun similarity(a: String, b: String): Float {
        if (a == b) return 1f
        if (a.contains(b) || b.contains(a)) return 0.8f

        val aTokens = a.split(" ").filter { it.isNotBlank() }.toSet()
        val bTokens = b.split(" ").filter { it.isNotBlank() }.toSet()
        if (aTokens.isEmpty() || bTokens.isEmpty()) return 0f

        val intersection = aTokens.intersect(bTokens).size.toFloat()
        val union = aTokens.union(bTokens).size.toFloat()
        return if (union == 0f) 0f else intersection / union
    }

    private fun normalize(value: String): String {
        val noAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return noAccents
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
