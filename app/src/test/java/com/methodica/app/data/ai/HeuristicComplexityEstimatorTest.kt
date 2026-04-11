package com.methodica.app.data.ai

import com.methodica.app.domain.ai.model.ParsedDocument
import com.methodica.app.domain.model.Topic
import org.junit.Assert.assertTrue
import org.junit.Test

class HeuristicComplexityEstimatorTest {

    private val estimator = HeuristicComplexityEstimator()

    @Test
    fun `incrementa complejidad cuando detecta senales tecnicas`() {
        val parsed = ParsedDocument(
            sourceLabel = "Temario",
            plainText = """
                Tema 1: Límite y derivada = regla de la cadena
                ejercicios de optimización y problema aplicado
            """.trimIndent(),
            sections = listOf("Tema 1"),
            detectedTopics = listOf("Tema 1"),
            examSignals = emptyList()
        )

        val topics = listOf(
            Topic(id = 1L, subjectId = 10L, name = "Tema 1", difficulty = 3, estimatedHours = 4, order = 0)
        )

        val result = kotlinx.coroutines.runBlocking {
            estimator.estimate(parsed, topics)
        }

        val topicAnalysis = result.topics.first()
        assertTrue(topicAnalysis.complexityLevel >= 4)
        assertTrue(topicAnalysis.estimatedHours >= 4)
        assertTrue(topicAnalysis.requiresPractice)
    }
}
