package com.methodica.app.data.ai

import com.methodica.app.domain.ai.LlmProvider
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ExternalLlmProvider @Inject constructor() : LlmProvider {

    private companion object {
        const val CONTENT_TYPE = "application/json"
        const val ANTHROPIC_VERSION = "2023-06-01"
    }

    override suspend fun generate(
        baseUrl: String,
        model: String,
        apiKey: String,
        prompt: String
    ): String? = withContext(Dispatchers.IO) {
        val request = buildRequest(
            baseUrl = baseUrl,
            model = model,
            apiKey = apiKey,
            prompt = prompt
        )

        val connection = (URL(request.url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", CONTENT_TYPE)
            request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }

        try {
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(request.payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val rawResponse = stream?.let { BufferedReader(it.reader()).use { reader -> reader.readText() } }.orEmpty()

            if (responseCode !in 200..299) {
                val details = extractErrorMessage(rawResponse)
                throw IllegalStateException("Error HTTP $responseCode al conectar con proveedor IA${details?.let { ": $it" } ?: ""}")
            }

            if (rawResponse.isBlank()) return@withContext null
            return@withContext extractContent(rawResponse, request.providerType)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildRequest(
        baseUrl: String,
        model: String,
        apiKey: String,
        prompt: String
    ): ProviderRequest {
        val normalizedBaseUrl = baseUrl.trim().removeSuffix("/")

        return when {
            normalizedBaseUrl.contains("api.anthropic.com", ignoreCase = true) -> {
                val url = if (normalizedBaseUrl.endsWith("/v1/messages")) {
                    normalizedBaseUrl
                } else {
                    "$normalizedBaseUrl/v1/messages"
                }
                ProviderRequest(
                    url = url,
                    providerType = ProviderType.ANTHROPIC,
                    headers = mapOf(
                        "x-api-key" to apiKey,
                        "anthropic-version" to ANTHROPIC_VERSION
                    ),
                    payload = JSONObject()
                        .put("model", model)
                        .put("max_tokens", 64)
                        .put(
                            "messages",
                            JSONArray().put(
                                JSONObject()
                                    .put("role", "user")
                                    .put("content", prompt)
                            )
                        )
                )
            }

            normalizedBaseUrl.contains("generativelanguage.googleapis.com", ignoreCase = true) ||
                normalizedBaseUrl.contains(":generateContent") -> {
                val endpoint = when {
                    normalizedBaseUrl.contains("{model}") -> normalizedBaseUrl.replace("{model}", model)
                    normalizedBaseUrl.contains(":generateContent") -> normalizedBaseUrl
                    else -> "$normalizedBaseUrl/v1beta/models/$model:generateContent"
                }
                val encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8.toString())
                val separator = if (endpoint.contains("?")) "&" else "?"
                ProviderRequest(
                    url = "$endpoint${separator}key=$encodedKey",
                    providerType = ProviderType.GEMINI,
                    headers = emptyMap(),
                    payload = JSONObject().put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", prompt))
                            )
                        )
                    )
                )
            }

            else -> {
                val endpoint = when {
                    normalizedBaseUrl.endsWith("/chat/completions") ||
                        normalizedBaseUrl.endsWith("/v1/messages") ||
                        normalizedBaseUrl.contains(":generateContent") -> normalizedBaseUrl
                    normalizedBaseUrl.endsWith("/openai") -> "$normalizedBaseUrl/v1/chat/completions"
                    else -> "$normalizedBaseUrl/chat/completions"
                }
                ProviderRequest(
                    url = endpoint,
                    providerType = ProviderType.OPENAI_COMPATIBLE,
                    headers = mapOf("Authorization" to "Bearer $apiKey"),
                    payload = JSONObject()
                        .put("model", model)
                        .put(
                            "messages",
                            JSONArray()
                                .put(
                                    JSONObject()
                                        .put("role", "system")
                                        .put("content", "Eres un asistente académico conciso.")
                                )
                                .put(
                                    JSONObject()
                                        .put("role", "user")
                                        .put("content", prompt)
                                )
                        )
                )
            }
        }
    }

    private fun extractContent(rawResponse: String, providerType: ProviderType): String? {
        val root = JSONObject(rawResponse)
        val content = when (providerType) {
            ProviderType.OPENAI_COMPATIBLE -> root
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")

            ProviderType.ANTHROPIC -> root
                .optJSONArray("content")
                ?.optJSONObject(0)
                ?.optString("text")

            ProviderType.GEMINI -> root
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
        }

        return content?.takeIf { it.isNotBlank() }
    }

    private fun extractErrorMessage(rawResponse: String): String? {
        if (rawResponse.isBlank()) return null
        return runCatching {
            val root = JSONObject(rawResponse)
            root.optJSONObject("error")?.optString("message")
                ?: root.optString("message")
                ?: rawResponse
        }.getOrElse {
            rawResponse
        }?.takeIf { it.isNotBlank() }
    }

    private data class ProviderRequest(
        val url: String,
        val providerType: ProviderType,
        val headers: Map<String, String>,
        val payload: JSONObject
    )

    private enum class ProviderType {
        OPENAI_COMPATIBLE,
        ANTHROPIC,
        GEMINI
    }
}

