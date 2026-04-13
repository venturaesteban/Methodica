package com.methodica.app.data.localai.provider

import android.content.Context
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.methodica.app.data.localai.runtime.LocalAiModelCatalog
import com.methodica.app.domain.ai.local.ChunkEmbedding
import com.methodica.app.domain.ai.local.EmbeddingProvider
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
    @Volatile
    private var loadedModelPath: String? = null

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
        val modelFile = context.filesDir.resolve(EMBEDDING_SPEC.localRelativePath)
        textEmbedder?.takeIf { loadedModelPath == modelFile.absolutePath && modelFile.exists() }?.let { return it }
        textEmbedder?.close()
        textEmbedder = null

        runtimeManager.ensureModelReady(EMBEDDING_SPEC).getOrThrow()
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
        loadedModelPath = modelFile.absolutePath
        created
    }

    companion object {
        private const val TFLITE_EXTENSION = ".tflite"

        val EMBEDDING_SPEC = LocalAiModelCatalog.definitionFor(LocalAiModelType.EMBEDDING_GEMMA).spec
    }
}
