package com.methodica.app.data.ai

import com.methodica.app.domain.ai.model.ParsedDocument
import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.AssessmentType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeuristicExamScopeInferenceServiceTest {

    private val service = HeuristicExamScopeInferenceService()

    @Test
    fun `alta confianza cuando hay senales explicitas de examen`() {
        val parsed = ParsedDocument(
            sourceLabel = "Guía",
            plainText = "Contenido evaluable: temas 1, 2 y 3",
            sections = listOf("Temario", "Evaluación final"),
            detectedTopics = listOf("Tema 1", "Tema 2", "Tema 3"),
            examSignals = listOf("Contenido evaluable", "Prueba final", "Examen")
        )
        val assessment = Assessment(
            id = 1L,
            subjectId = 10L,
            type = AssessmentType.EXAM,
            title = "Parcial",
            date = System.currentTimeMillis() + 86400000L
        )

        val result = kotlinx.coroutines.runBlocking {
            service.infer(assessment, parsed)
        }

        assertTrue(result.confidence >= 0.8f)
        assertFalse(result.requiresConfirmation)
        assertTrue(result.includedTopicNames.isNotEmpty())
    }

    @Test
    fun `requiere confirmacion cuando no hay senales explicitas`() {
        val parsed = ParsedDocument(
            sourceLabel = "Apuntes",
            plainText = "Resumen general de clase",
            sections = listOf("Resumen"),
            detectedTopics = listOf("Bloque A"),
            examSignals = emptyList()
        )
        val assessment = Assessment(
            id = 2L,
            subjectId = 10L,
            type = AssessmentType.EXAM,
            title = "Final",
            date = System.currentTimeMillis() + 86400000L
        )

        val result = kotlinx.coroutines.runBlocking {
            service.infer(assessment, parsed)
        }

        assertTrue(result.requiresConfirmation)
        assertTrue(result.confidence < 0.8f)
    }
}
