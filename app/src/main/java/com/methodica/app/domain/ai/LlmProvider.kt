package com.methodica.app.domain.ai

@Deprecated("Legacy remote provider. Use local ai contracts and AiWorkflowCoordinator.")
interface LlmProvider {
    suspend fun generate(
        baseUrl: String,
        model: String,
        apiKey: String,
        prompt: String
    ): String?
}
