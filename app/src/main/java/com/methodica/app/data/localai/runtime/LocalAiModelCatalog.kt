package com.methodica.app.data.localai.runtime

import com.methodica.app.domain.ai.local.DownloadableLocalModelDescriptor
import com.methodica.app.domain.ai.local.LocalAiModelSpec
import com.methodica.app.domain.ai.local.LocalAiModelType

data class LocalAiModelDefinition(
    val spec: LocalAiModelSpec,
    val fallbackDownload: DownloadableLocalModelDescriptor? = null
)

internal object LocalAiModelCatalog {
    private val embeddingDefinition = LocalAiModelDefinition(
        spec = LocalAiModelSpec(
            id = "embeddinggemma-300m-seq1024",
            type = LocalAiModelType.EMBEDDING_GEMMA,
            displayName = "EmbeddingGemma",
            version = "embeddinggemma-300m-textembedder-tflite-seq1024-v1",
            localRelativePath = "local_models/embeddinggemma/embeddinggemma-300M_seq1024_mixed-precision.tflite",
            requiredDiskBytes = 256L * 1024L * 1024L,
            requiredRamMb = 256,
            expectedSha256 = "8b0b8bbd0aa95f9f747c25a6c87cd05a8286933282660f6a50da877662917e31"
        ),
        fallbackDownload = DownloadableLocalModelDescriptor(
            id = "embeddinggemma-300m-seq1024",
            version = "embeddinggemma-300m-textembedder-tflite-seq1024-v1",
            downloadUrl = "https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/embeddinggemma-300M_seq1024_mixed-precision.tflite?download=true",
            sha256 = "8b0b8bbd0aa95f9f747c25a6c87cd05a8286933282660f6a50da877662917e31",
            sizeBytes = 183_329_528L,
            requiredRamMb = 256,
            requiredDiskBytes = 256L * 1024L * 1024L,
            supportedAbis = listOf("arm64-v8a"),
            minSdk = 26
        )
    )

    private val gemmaDefinition = LocalAiModelDefinition(
        spec = LocalAiModelSpec(
            id = "gemma-3n-e2b-it-int4-litertlm",
            type = LocalAiModelType.GEMMA_3N_REASONING,
            displayName = "Gemma 3n",
            version = "gemma-3n-e2b-it-int4-litertlm-v1",
            localRelativePath = "local_models/gemma3n/gemma-3n-E2B-it-int4.litertlm",
            requiredDiskBytes = 4_500L * 1024L * 1024L,
            requiredRamMb = 4096,
            expectedSha256 = "2ed7bc3a0026c93d5b8a4544b352d9d00cd66ff0bac3ef6a20ac3d2cba4010d6",
            redistributionRequiresLicenseConfirmation = true
        )
    )

    val allDefinitions: List<LocalAiModelDefinition> = listOf(
        embeddingDefinition,
        gemmaDefinition
    )

    fun definitionFor(type: LocalAiModelType): LocalAiModelDefinition = allDefinitions.first { it.spec.type == type }
}
