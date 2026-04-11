package com.methodica.app.data.ai

import com.methodica.app.domain.ai.ExamScopeInferenceService
import com.methodica.app.domain.ai.model.ExamScopeInference
import com.methodica.app.domain.ai.model.ParsedDocument
import com.methodica.app.domain.model.Assessment
import javax.inject.Inject

class HeuristicExamScopeInferenceService @Inject constructor() : ExamScopeInferenceService {

    override suspend fun infer(
        assessment: Assessment,
        parsedDocument: ParsedDocument
    ): ExamScopeInference {
        val sureTopics = parsedDocument.detectedTopics.take(4)
        val probableTopics = parsedDocument.detectedTopics.drop(4).take(6)
        val excludedTopics = parsedDocument.detectedTopics.drop(10).take(4)

        val inferredTopics = (sureTopics + probableTopics).distinct()

        var confidence = 0.35f
        confidence += (parsedDocument.examSignals.size.coerceAtMost(5) * 0.12f)
        confidence += (parsedDocument.sections.size.coerceAtMost(6) * 0.03f)
        confidence += (parsedDocument.detectedTopics.size.coerceAtMost(10) * 0.025f)
        if (parsedDocument.qualityWarnings.isNotEmpty()) {
            confidence -= 0.08f
        }
        confidence = confidence.coerceIn(0.2f, 0.92f)

        val requiresConfirmation = confidence < 0.8f

        val scopeText = buildString {
            appendLine("Entra seguro:")
            if (sureTopics.isEmpty()) appendLine("- No identificado con alta certeza")
            else sureTopics.forEach { appendLine("- $it") }

            appendLine()
            appendLine("Probable:")
            if (probableTopics.isEmpty()) appendLine("- Sin señales adicionales")
            else probableTopics.forEach { appendLine("- $it") }

            appendLine()
            appendLine("No entra / no concluyente:")
            if (excludedTopics.isEmpty()) appendLine("- Sin exclusiones explícitas")
            else excludedTopics.forEach { appendLine("- $it") }
        }.trim()

        val justification = buildString {
            appendLine("Motivos de confianza:")
            appendLine("- Señales de evaluación detectadas: ${parsedDocument.examSignals.size}")
            appendLine("- Secciones estructurales detectadas: ${parsedDocument.sections.size}")
            appendLine("- Temas detectados: ${parsedDocument.detectedTopics.size}")
            if (parsedDocument.qualityWarnings.isNotEmpty()) {
                appendLine("- Advertencias de calidad:")
                parsedDocument.qualityWarnings.forEach { appendLine("  - $it") }
            }
            if (parsedDocument.examSignals.isEmpty()) {
                append("La inferencia se apoya en estructura general y requiere validación manual.")
            }
        }

        return ExamScopeInference(
            estimatedScope = scopeText,
            includedTopicNames = inferredTopics,
            justification = justification,
            confidence = confidence,
            requiresConfirmation = requiresConfirmation
        )
    }
}
