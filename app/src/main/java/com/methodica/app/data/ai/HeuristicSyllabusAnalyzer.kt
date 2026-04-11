package com.methodica.app.data.ai

import com.methodica.app.domain.ai.SyllabusAnalyzer
import com.methodica.app.domain.ai.model.ParsedDocument
import javax.inject.Inject

class HeuristicSyllabusAnalyzer @Inject constructor() : SyllabusAnalyzer {

    override suspend fun analyze(parsedDocument: ParsedDocument): ParsedDocument {
        val fallbackTopics = parsedDocument.plainText
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { it.length in 8..80 }
            .distinct()
            .take(10)

        return if (parsedDocument.detectedTopics.isNotEmpty()) {
            parsedDocument
        } else {
            parsedDocument.copy(detectedTopics = fallbackTopics)
        }
    }
}
