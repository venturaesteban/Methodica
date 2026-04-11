package com.methodica.app.data.localai.provider

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
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
        val options = TextEmbedderOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(modelFile.absolutePath)
                    .build()
            )
            .build()
        val created = TextEmbedder.createFromOptions(context, options)
        textEmbedder = created
        created
    }

    companion object {
        val EMBEDDING_SPEC = LocalAiModelSpec(
            id = "embeddinggemma-300m",
            type = LocalAiModelType.EMBEDDING_GEMMA,
            version = "embeddinggemma-300m-task-v1",
            assetPath = "models/embeddinggemma/embeddinggemma-300m.task",
            localRelativePath = "local_models/embeddinggemma/embeddinggemma-300m.task",
            requiredDiskBytes = 700L * 1024L * 1024L,
            requiredRamMb = 2048,
            expectedSha256 = null,
            downloadUrl = null
        )
    }
}
