package com.methodica.app.data.localai.provider

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.methodica.app.domain.ai.local.LocalAiModelSpec
import com.methodica.app.domain.ai.local.LocalAiModelType
import com.methodica.app.domain.ai.local.LocalModelRuntimeManager
import com.methodica.app.domain.ai.local.ReasoningPlanOutput
import com.methodica.app.domain.ai.local.ReasoningProvider
import com.methodica.app.domain.ai.local.ReasoningRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class GemmaLocalReasoningProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runtimeManager: LocalModelRuntimeManager,
    private val outputParser: GemmaReasoningOutputParser
) : ReasoningProvider {

    override suspend fun reason(request: ReasoningRequest): Result<ReasoningPlanOutput> =
        runCatching {
            runtimeManager.ensureModelReady(GEMMA_3N_SPEC).getOrElse {
                throw ReasoningRuntimeException.ModelNotFound(it.message ?: "Modelo Gemma 3n no disponible")
            }
            val modelPath = context.filesDir.resolve(GEMMA_3N_SPEC.localRelativePath).absolutePath
            val prompt = buildPrompt(request)
            val firstRaw = invokeLocalGemma(prompt = prompt, modelPath = modelPath)
            outputParser.parse(firstRaw).getOrElse {
                val repairPrompt = buildRepairPrompt(originalPrompt = prompt, invalidOutput = firstRaw, error = it.message)
                val repairedRaw = invokeLocalGemma(prompt = repairPrompt, modelPath = modelPath)
                outputParser.parse(repairedRaw).getOrThrow()
            }
        }.onFailure { failure ->
            val typed = failure.toReasoningRuntimeException()
            runtimeManager.markModelError(LocalAiModelType.GEMMA_3N_REASONING, typed.message ?: "Fallo runtime Gemma 3n")
        }

    private suspend fun invokeLocalGemma(prompt: String, modelPath: String): String = withContext(Dispatchers.Default) {
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setTopK(32)
                .setTemperature(0.2f)
                .build()

            LlmInference.createFromOptions(context, options).use { engine ->
                val generated = engine.generateResponse(prompt)
                generated?.takeIf { it.isNotBlank() }
                    ?: throw ReasoningRuntimeException.InferenceError("Gemma 3n no devolvió contenido utilizable")
            }
        } catch (exception: NoClassDefFoundError) {
            throw ReasoningRuntimeException.ClassNotFound("Runtime MediaPipe GenAI ausente en tiempo de ejecución", exception)
        } catch (exception: NoSuchMethodError) {
            throw ReasoningRuntimeException.MissingMethod("API incompatible de LlmInference: método ausente", exception)
        } catch (exception: UnsatisfiedLinkError) {
            throw ReasoningRuntimeException.RuntimeIncompatibility(
                "No se pudo cargar JNI del runtime GenAI (ABI/dispositivo incompatible)",
                exception
            )
        } catch (exception: IllegalStateException) {
            throw ReasoningRuntimeException.InitializationFailed(
                "Fallo al inicializar LlmInference. Revisa modelo .task compatible con tasks-genai.",
                exception
            )
        } catch (exception: RuntimeException) {
            throw ReasoningRuntimeException.InferenceError("Error de inferencia Gemma local: ${exception.message}", exception)
        }
    }

    private fun buildPrompt(request: ReasoningRequest): String {
        val evidenceJson = JSONArray().apply {
            request.evidence.forEach { hit ->
                put(
                    JSONObject()
                        .put("chunkExternalId", hit.chunkExternalId)
                        .put("score", "%.4f".format(hit.score))
                        .put("content", hit.content.take(700))
                        .put("subjectId", hit.source.subjectId)
                        .put("assessmentId", hit.source.assessmentId)
                        .put("topicId", hit.source.topicId)
                        .put("materialId", hit.source.materialId)
                        .put("documentId", hit.source.documentId)
                )
            }
        }

        return """
            Eres Gemma 3n ejecutándose localmente en Android para Methodica.
            Analiza SOLO la evidencia recuperada y el objetivo de evaluación.
            No inventes temas fuera de la evidencia salvo para advertir carencias.
            Responde EXCLUSIVAMENTE con JSON válido (sin markdown).

            Esquema JSON requerido:
            {
              "scope": {
                "estimatedScope": "string",
                "justification": "string",
                "confidence": 0.0,
                "requiresUserConfirmation": false
              },
              "topicComplexities": [
                {
                  "topicName": "string",
                  "complexityLevel": 1,
                  "recommendedHours": 1,
                  "priority": 1,
                  "isIncludedInScope": true,
                  "requiresPractice": true,
                  "requiresSpacedReview": false,
                  "rationale": "string"
                }
              ],
              "sequencing": [
                {
                  "order": 1,
                  "topicName": "string",
                  "why": "string"
                }
              ],
              "risks": ["string"],
              "summary": "string",
              "confidence": 0.0
            }

            Objetivo:
            ${request.prompt}

            Evidencia local relevante:
            $evidenceJson
        """.trimIndent()
    }

    private fun buildRepairPrompt(originalPrompt: String, invalidOutput: String, error: String?): String = """
        La salida anterior no cumple el JSON requerido.
        Error de validación: ${error ?: "desconocido"}
        Devuelve SOLO JSON válido con el mismo esquema.

        Prompt original:
        $originalPrompt

        Salida inválida previa:
        $invalidOutput
    """.trimIndent()

    companion object {
        val GEMMA_3N_SPEC = LocalAiModelSpec(
            id = "gemma-3n-e2b-it-int4",
            type = LocalAiModelType.GEMMA_3N_REASONING,
            version = "gemma-3n-e2b-it-int4-v1",
            assetPath = "models/gemma3n/gemma-3n-e2b-it-int4.task",
            localRelativePath = "local_models/gemma3n/gemma-3n-e2b-it-int4.task",
            requiredDiskBytes = 4_500L * 1024L * 1024L,
            requiredRamMb = 4096,
            expectedSha256 = null,
            downloadUrl = null
        )
    }
}

private sealed class ReasoningRuntimeException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause) {
    class ClassNotFound(message: String, cause: Throwable? = null) : ReasoningRuntimeException(message, cause)
    class MissingMethod(message: String, cause: Throwable? = null) : ReasoningRuntimeException(message, cause)
    class InitializationFailed(message: String, cause: Throwable? = null) : ReasoningRuntimeException(message, cause)
    class ModelNotFound(message: String, cause: Throwable? = null) : ReasoningRuntimeException(message, cause)
    class InferenceError(message: String, cause: Throwable? = null) : ReasoningRuntimeException(message, cause)
    class RuntimeIncompatibility(message: String, cause: Throwable? = null) : ReasoningRuntimeException(message, cause)
}

private fun Throwable.toReasoningRuntimeException(): ReasoningRuntimeException = when (this) {
    is ReasoningRuntimeException -> this
    is NoClassDefFoundError -> ReasoningRuntimeException.ClassNotFound("ClassNotFound en runtime GenAI", this)
    is NoSuchMethodError -> ReasoningRuntimeException.MissingMethod("Método ausente en runtime GenAI", this)
    is UnsatisfiedLinkError -> ReasoningRuntimeException.RuntimeIncompatibility("Runtime GenAI no compatible con el dispositivo", this)
    else -> ReasoningRuntimeException.InferenceError(message ?: "Fallo no clasificado en Gemma local", this)
}
