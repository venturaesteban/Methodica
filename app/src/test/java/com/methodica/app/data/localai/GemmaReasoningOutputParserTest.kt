package com.methodica.app.data.localai

import com.methodica.app.data.localai.provider.GemmaReasoningOutputParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaReasoningOutputParserTest {

    private val parser = GemmaReasoningOutputParser()

    @Test
    fun `parsea salida json valida`() {
        val raw = """
            {
              "scope": {
                "estimatedScope": "Temas 1 al 4 con foco en ejercicios",
                "justification": "Coincide con evidencias de examen y guias",
                "confidence": 0.82,
                "requiresUserConfirmation": false
              },
              "topicComplexities": [
                {
                  "topicName": "Derivadas",
                  "complexityLevel": 4,
                  "recommendedHours": 6,
                  "priority": 5,
                  "isIncludedInScope": true,
                  "requiresPractice": true,
                  "requiresSpacedReview": true,
                  "rationale": "Alta densidad de ejercicios"
                }
              ],
              "sequencing": [
                {"order": 1, "topicName": "Limites", "why": "Prerequisito"}
              ],
              "risks": ["Poco tiempo para simulacros"],
              "summary": "Plan centrado en núcleo evaluable",
              "confidence": 0.8
            }
        """.trimIndent()

        val result = parser.parse(raw)

        assertTrue(result.isSuccess)
        assertEquals("Derivadas", result.getOrThrow().topicComplexities.first().topicName)
    }

    @Test
    fun `falla si no hay topics incluidos`() {
        val raw = """
            {
              "scope": {
                "estimatedScope": "Temas sueltos",
                "justification": "Sin base",
                "confidence": 0.2,
                "requiresUserConfirmation": true
              },
              "topicComplexities": [],
              "sequencing": [],
              "risks": [],
              "summary": "x",
              "confidence": 0.2
            }
        """.trimIndent()

        val result = parser.parse(raw)
        assertTrue(result.isFailure)
    }
}
