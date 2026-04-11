package com.methodica.app.domain.ai

import com.methodica.app.domain.ai.model.ParsedDocument

interface SyllabusAnalyzer {
    suspend fun analyze(parsedDocument: ParsedDocument): ParsedDocument
}
