package com.methodica.app.domain.usecase.ai

import com.methodica.app.domain.ai.LlmProvider
import javax.inject.Inject

class VerifyAiConnectionUseCase @Inject constructor(
    private val llmProvider: LlmProvider
) {
    suspend operator fun invoke(
        baseUrl: String,
        model: String,
        apiKey: String
    ): Result<String> = runCatching {
        // Validar que los datos no estén vacíos
        if (baseUrl.isBlank() || model.isBlank() || apiKey.isBlank()) {
            throw IllegalArgumentException("Proveedor, URL, modelo y API Key son requeridos")
        }

        // Hacer una solicitud simple de prueba
        val testPrompt = "Responde solo con la palabra 'OK' para confirmar que estás funcionando."
        val response = llmProvider.generate(baseUrl, model, apiKey, testPrompt)
            ?: throw RuntimeException("No se recibió respuesta del proveedor de IA")

        if (response.isBlank()) {
            throw RuntimeException("Respuesta vacía del proveedor de IA")
        }

        response
    }
}

