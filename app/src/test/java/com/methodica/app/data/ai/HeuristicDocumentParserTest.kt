package com.methodica.app.data.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeuristicDocumentParserTest {

    private val parser = HeuristicDocumentParser()

    @Test
    fun `bloquea documentos demasiado cortos`() = runBlocking {
        val parsed = parser.parse(
            sourceLabel = "Texto breve",
            rawText = "Tema 1. Introducción."
        )

        assertNotNull(parsed.blockedReason)
    }

    @Test
    fun `acepta documento estructurado y genera señales`() = runBlocking {
        val rawText = """
            Tema 1. Álgebra lineal
            Tema 2. Espacios vectoriales
            Tema 3. Matrices
            Tema 4. Determinantes
            Tema 5. Diagonalización
            Criterios de evaluación: ejercicios tipo examen y prueba final.
            Contenido evaluable: temas 1 al 5.
            Objetivos y temario del curso.
        """.trimIndent().repeat(5)

        val parsed = parser.parse(sourceLabel = "Guía docente", rawText = rawText)

        assertNull(parsed.blockedReason)
        assertTrue(parsed.sections.isNotEmpty())
        assertTrue(parsed.detectedTopics.size >= 5)
        assertTrue(parsed.examSignals.size >= 2)
        assertTrue(parsed.wordCount >= 120)
    }
}

