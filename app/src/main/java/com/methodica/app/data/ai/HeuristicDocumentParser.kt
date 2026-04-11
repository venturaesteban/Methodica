package com.methodica.app.data.ai

import com.methodica.app.domain.ai.DocumentParser
import com.methodica.app.domain.ai.model.ParsedDocument
import javax.inject.Inject

class HeuristicDocumentParser @Inject constructor() : DocumentParser {

    private val examKeywords = listOf(
        "entra", "contenido evaluable", "temario", "objetivos", "criterios", "evaluación",
        "prueba final", "examen", "ponderación", "tipo test", "pregunta", "rúbrica", "rubrica"
    )

    private companion object {
        const val MIN_WORDS = 120
        const val MAX_WORDS = 12_000
    }

    override suspend fun parse(sourceLabel: String, rawText: String): ParsedDocument {
        val normalized = normalizeText(rawText)
        val lines = normalized
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val sectionRegex = Regex(
            pattern = "^(#\\s*)?((tema|unidad|bloque|cap[ií]tulo)\\s*\\d+|[ivxlcdm]+[.)]|\\d+[.)]).*",
            option = RegexOption.IGNORE_CASE
        )

        val sections = lines.filter { line ->
            line.startsWith("#") ||
                line.matches(sectionRegex) ||
                (line.length in 6..80 && line.uppercase() == line && line.any { it.isLetter() })
        }.ifEmpty { lines.take(8) }

        val topicRegex = Regex("^(#\\s*)?(tema|unidad|bloque|cap[ií]tulo)\\s*\\d+[:.)-]?\\s*.*$", RegexOption.IGNORE_CASE)
        val numberedTopicRegex = Regex("^\\d+[.)-]\\s+.*")

        val detectedTopics = lines
            .asSequence()
            .filter { it.length in 8..140 }
            .filter { line ->
                line.matches(topicRegex) ||
                    line.matches(numberedTopicRegex) ||
                    line.contains("tema", ignoreCase = true)
            }
            .map { it.removePrefix("#").trim().trimEnd(':', '-', '.', ';') }
            .distinct()
            .take(18)
            .toList()

        val examSignals = lines
            .filter { line -> examKeywords.any { keyword -> line.contains(keyword, ignoreCase = true) } }
            .distinct()
            .take(20)

        val wordCount = normalized
            .split(Regex("\\s+"))
            .count { it.isNotBlank() }

        val qualityWarnings = buildList {
            if (sections.size < 3) add("Estructura débil: se detectaron pocos encabezados/secciones.")
            if (detectedTopics.size < 5) add("Temario poco explícito: se detectaron menos de 5 temas claros.")
            if (examSignals.size < 2) add("Faltan señales explícitas de evaluación (alcance incierto).")
        }

        val blockedReason = when {
            wordCount < MIN_WORDS -> "Documento demasiado corto para inferir alcance fiable (mínimo $MIN_WORDS palabras)."
            wordCount > MAX_WORDS -> "Documento demasiado extenso para análisis heurístico directo (máximo $MAX_WORDS palabras)."
            else -> null
        }

        return ParsedDocument(
            sourceLabel = sourceLabel,
            plainText = normalized,
            sections = sections,
            detectedTopics = detectedTopics,
            examSignals = examSignals,
            wordCount = wordCount,
            qualityWarnings = qualityWarnings,
            blockedReason = blockedReason
        )
    }

    private fun normalizeText(rawText: String): String {
        return rawText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { line ->
                line
                    .replace(Regex("\\s+"), " ")
                    .replace(Regex("^p[aá]gina\\s+\\d+\\s*$", RegexOption.IGNORE_CASE), "")
                    .trim()
            }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .trim()
    }
}
