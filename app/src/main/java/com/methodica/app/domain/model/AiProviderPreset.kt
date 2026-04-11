package com.methodica.app.domain.model

data class AiProviderPreset(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val authType: AuthType,
    val suggestedModels: List<String>,
    val documentation: String
) {
    enum class AuthType {
        BEARER_TOKEN,           // Authorization: Bearer {api_key}
        API_KEY_HEADER,         // x-api-key: {api_key}
        URL_PARAMETER          // ?key={api_key}
    }
}

object AiProviderPresets {
    val providers = listOf(
        AiProviderPreset(
            id = "openai",
            displayName = "OpenAI",
            baseUrl = "https://api.openai.com/v1/chat/completions",
            authType = AiProviderPreset.AuthType.BEARER_TOKEN,
            suggestedModels = listOf(
                "gpt-5.4",
                "gpt-5.4-mini",
                "gpt-5.4-nano",
                "gpt-4o-mini"
            ),
            documentation = "https://developers.openai.com/api/docs/models"
        ),
        AiProviderPreset(
            id = "anthropic",
            displayName = "Anthropic Claude",
            baseUrl = "https://api.anthropic.com/v1/messages",
            authType = AiProviderPreset.AuthType.API_KEY_HEADER,
            suggestedModels = listOf(
                "claude-opus-4-6",
                "claude-sonnet-4-6",
                "claude-haiku-4-5"
            ),
            documentation = "https://platform.claude.com/docs/en/docs/about-claude/models"
        ),
        AiProviderPreset(
            id = "gemini",
            displayName = "Google Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent",
            authType = AiProviderPreset.AuthType.URL_PARAMETER,
            suggestedModels = listOf(
                "gemini-3.1-pro-preview",
                "gemini-3-flash-preview",
                "gemini-3.1-flash-lite-preview",
                "gemini-2.5-flash",
                "gemini-2.5-pro",
                "gemini-2.5-flash-lite"
            ),
            documentation = "https://ai.google.dev/gemini-api/docs/models"
        ),
        AiProviderPreset(
            id = "deepseek",
            displayName = "DeepSeek AI",
            baseUrl = "https://api.deepseek.com/chat/completions",
            authType = AiProviderPreset.AuthType.BEARER_TOKEN,
            suggestedModels = listOf(
                "deepseek-chat",
                "deepseek-reasoner"
            ),
            documentation = "https://platform.deepseek.com/docs"
        ),
        AiProviderPreset(
            id = "grok",
            displayName = "Grok (xAI)",
            baseUrl = "https://api.x.ai/v1/chat/completions",
            authType = AiProviderPreset.AuthType.BEARER_TOKEN,
            suggestedModels = listOf(
                "grok-3",
                "grok-3-mini",
                "grok-beta"
            ),
            documentation = "https://docs.x.ai"
        )
    )

    fun getById(id: String): AiProviderPreset? = providers.find { it.id == id }
    fun getByName(name: String): AiProviderPreset? = providers.find { it.displayName == name }
}

