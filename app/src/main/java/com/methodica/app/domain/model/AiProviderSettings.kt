package com.methodica.app.domain.model

data class AiProviderSettings(
    val externalEnabled: Boolean = false,
    val providerName: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = ""
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() && apiKey.isNotBlank()

    val isEnabledAndConfigured: Boolean
        get() = externalEnabled && isConfigured
}

