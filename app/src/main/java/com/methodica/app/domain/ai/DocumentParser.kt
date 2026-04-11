package com.methodica.app.domain.ai

import com.methodica.app.domain.ai.model.ParsedDocument

interface DocumentParser {
    suspend fun parse(
        sourceLabel: String,
        rawText: String
    ): ParsedDocument
}
