package com.methodica.app.domain.ai.model

data class ParsedDocument(
    val sourceLabel: String,
    val plainText: String,
    val sections: List<String>,
    val detectedTopics: List<String>,
    val examSignals: List<String>,
    val wordCount: Int = 0,
    val qualityWarnings: List<String> = emptyList(),
    val blockedReason: String? = null
)
