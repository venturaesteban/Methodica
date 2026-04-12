package com.methodica.app.data.localai.provider

import android.content.Context
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.methodica.app.domain.ai.local.ChunkEmbedding
import com.methodica.app.domain.ai.local.EmbeddingProvider
import com.methodica.app.domain.ai.local.LocalAiModelSpec
import com.methodica.app.domain.ai.local.LocalAiModelType
import com.methodica.app.domain.ai.local.LocalModelRuntimeManager
import com.methodica.app.domain.ai.local.TextChunk
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class MediaPipeTextEmbeddingProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runtimeManager: LocalModelRuntimeManager
) : EmbeddingProvider {

    override val modelVersion: String = EMBEDDING_SPEC.version

    private val mutex = Mutex()
    @Volatile
    private var textEmbedder: TextEmbedder? = null

    override suspend fun embed(chunks: List<TextChunk>): Result<List<ChunkEmbedding>> = runCatching {
        if (chunks.isEmpty()) return@runCatching emptyList()
        val embedder = ensureEmbedder()
        chunks.map { chunk ->
            val result = embedder.embed(chunk.content)
            val vector = result.embeddingResult().embeddings().firstOrNull()?.floatEmbedding()
                ?: error("El modelo no devolvió embeddings para el chunk ${chunk.externalId}")
            ChunkEmbedding(
                chunkExternalId = chunk.externalId,
                dimensions = vector.size,
                values = vector
            )
        }
    }

    suspend fun warmUp(): Result<Unit> = runCatching {
        ensureEmbedder()
    }

    private suspend fun ensureEmbedder(): TextEmbedder = mutex.withLock {
        textEmbedder?.let { return it }
        runtimeManager.ensureModelReady(EMBEDDING_SPEC).getOrThrow()
        val modelFile = context.filesDir.resolve(EMBEDDING_SPEC.localRelativePath)
        if (!modelFile.name.endsWith(TFLITE_EXTENSION, ignoreCase = true)) {
            val message = "TextEmbedder requiere un modelo .tflite y Methodica encontro ${modelFile.name}."
            runtimeManager.markModelError(LocalAiModelType.EMBEDDING_GEMMA, message)
            error(message)
        }
        val created = runCatching {
            TextEmbedder.createFromFile(context, modelFile)
        }.getOrElse { cause ->
            val detail = cause.message?.takeIf { it.isNotBlank() } ?: "sin detalle"
            val message = buildString {
                append("No se pudo inicializar TextEmbedder con ")
                append(modelFile.name)
                append(". El artefacto debe ser un .tflite compatible con MediaPipe Text Embedder")
                append(" y, si usa tensores int32, incluir metadatos/tokenizacion compatibles.")
                append(" Detalle: ")
                append(detail)
            }
            runtimeManager.markModelError(LocalAiModelType.EMBEDDING_GEMMA, message)
            throw IllegalStateException(message, cause)
        }
        textEmbedder = created
        created
    }

    companion object {
        private const val MODEL_DIRECTORY = "embeddinggemma"
        private const val MODEL_FILENAME = "embeddinggemma-300M_seq1024_mixed-precision.tflite"
        private const val TFLITE_EXTENSION = ".tflite"

        val EMBEDDING_SPEC = LocalAiModelSpec(
            id = "embeddinggemma-300m-seq1024",
            type = LocalAiModelType.EMBEDDING_GEMMA,
            version = "embeddinggemma-300m-textembedder-tflite-seq1024-v1",
            assetPath = "models/$MODEL_DIRECTORY/$MODEL_FILENAME",
            localRelativePath = "local_models/$MODEL_DIRECTORY/$MODEL_FILENAME",
            requiredDiskBytes = 256L * 1024L * 1024L,
            requiredRamMb = 256,
            expectedSha256 = null,
            downloadUrl = null
        )
    }
}
