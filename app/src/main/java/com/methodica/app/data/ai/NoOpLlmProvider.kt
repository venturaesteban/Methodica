package com.methodica.app.data.ai

import com.methodica.app.domain.ai.LlmProvider
import javax.inject.Inject

class NoOpLlmProvider @Inject constructor() : LlmProvider {
    override suspend fun generate(
        baseUrl: String,
        model: String,
        apiKey: String,
        prompt: String
    ): String? = null
}
