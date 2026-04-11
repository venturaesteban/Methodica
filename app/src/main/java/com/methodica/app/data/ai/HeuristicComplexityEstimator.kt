package com.methodica.app.data.ai

import com.methodica.app.domain.ai.ComplexityEstimator
import com.methodica.app.domain.ai.model.ComplexityAnalysis
import com.methodica.app.domain.ai.model.ParsedDocument
import com.methodica.app.domain.ai.model.TopicAnalysis
import com.methodica.app.domain.model.Topic
import javax.inject.Inject
import kotlin.math.roundToInt

class HeuristicComplexityEstimator @Inject constructor() : ComplexityEstimator {

    override suspend fun estimate(
        parsedDocument: ParsedDocument,
        topics: List<Topic>
    ): ComplexityAnalysis {
        val textLines = parsedDocument.plainText.lines()
        val analyses = topics.sortedBy { it.order }.mapIndexed { index, topic ->
            val blockText = textLines
                .firstOrNull { it.contains(topic.name, ignoreCase = true) }
                .orEmpty()

            val formulaSignal = blockText.contains("=", ignoreCase = false) || blockText.contains("derivar", ignoreCase = true)
            val technicalSignal = blockText.split(" ").count { it.length > 10 } >= 2 ||
                blockText.contains("teorema", ignoreCase = true) ||
                blockText.contains("demostr", ignoreCase = true)
            val practiceSignal = blockText.contains("ejercicio", ignoreCase = true) || blockText.contains("problema", ignoreCase = true)

            val base = topic.difficulty.coerceIn(1, 3)
            val bonus = (if (formulaSignal) 1 else 0) + (if (technicalSignal) 1 else 0)
            val complexity = (base + bonus).coerceIn(1, 5)

            val multiplier = (1f + ((complexity - 3) * 0.15f)).coerceIn(0.85f, 1.6f)
            val estimatedHours = (topic.estimatedHours * multiplier)
                .roundToInt()
                .coerceIn(1, (topic.estimatedHours * 2).coerceAtLeast(2))
            val priority = (100 - index * 5 + complexity * 6).coerceAtLeast(1)

            TopicAnalysis(
                topicName = topic.name,
                complexityLevel = complexity,
                estimatedHours = estimatedHours,
                priority = priority,
                requiresPractice = practiceSignal || complexity >= 4,
                requiresSpacedReview = complexity >= 4,
                rationale = "Complejidad inferida por dificultad declarada y señales textuales del contenido.",
                confidence = if (blockText.isBlank()) 0.55f else 0.72f
            )
        }

        val overall = if (analyses.isEmpty()) 1 else analyses.map { it.complexityLevel }.average().roundToInt().coerceIn(1, 5)
        val confidence = if (analyses.isEmpty()) 0.4f else analyses.map { it.confidence }.average().toFloat()

        return ComplexityAnalysis(
            topics = analyses,
            overallComplexity = overall,
            confidence = confidence
        )
    }
}
