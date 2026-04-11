package com.methodica.app.domain.ai

interface LlmProvider {
    suspend fun generate(
        baseUrl: String,
        model: String,
        apiKey: String,
        prompt: String
    ): String?
}
