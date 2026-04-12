package com.methodica.app.data.localai.provider

import android.content.Context
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

    override suspend fun reason(request: ReasoningRequest): Result<ReasoningPlanOutput> = runCatching {
        runtimeManager.ensureModelReady(GEMMA_3N_SPEC).getOrThrow()
        val modelPath = context.filesDir.resolve(GEMMA_3N_SPEC.localRelativePath).absolutePath
        val prompt = buildPrompt(request)
        val firstRaw = invokeLocalGemma(prompt = prompt, modelPath = modelPath)
        outputParser.parse(firstRaw).getOrElse {
            val repairPrompt = buildRepairPrompt(originalPrompt = prompt, invalidOutput = firstRaw, error = it.message)
            val repairedRaw = invokeLocalGemma(prompt = repairPrompt, modelPath = modelPath)
            outputParser.parse(repairedRaw).getOrThrow()
        }
    }

    private suspend fun invokeLocalGemma(prompt: String, modelPath: String): String = withContext(Dispatchers.Default) {
        val llmClass = Class.forName("com.google.mediapipe.tasks.genai.llminference.LlmInference")
        val optionsClass = Class.forName("com.google.mediapipe.tasks.genai.llminference.LlmInference\$LlmInferenceOptions")

        val optionsBuilder = optionsClass.getMethod("builder").invoke(null)
        optionsBuilder.javaClass.getMethod("setModelPath", String::class.java).invoke(optionsBuilder, modelPath)
        runCatching { optionsBuilder.javaClass.getMethod("setMaxTokens", Int::class.javaPrimitiveType).invoke(optionsBuilder, 1024) }
        runCatching { optionsBuilder.javaClass.getMethod("setTopK", Int::class.javaPrimitiveType).invoke(optionsBuilder, 32) }
        runCatching { optionsBuilder.javaClass.getMethod("setTemperature", Float::class.javaPrimitiveType).invoke(optionsBuilder, 0.2f) }

        val options = optionsBuilder.javaClass.getMethod("build").invoke(optionsBuilder)
        val create = llmClass.methods.firstOrNull { method ->
            method.name == "createFromOptions" && method.parameterTypes.size == 2
        } ?: error("No se encontró createFromOptions para LlmInference")

        val engine = create.invoke(null, context, options)
        val generate = llmClass.methods.firstOrNull { method ->
            method.name == "generateResponse" && method.parameterTypes.size == 1
        } ?: error("No se encontró generateResponse para LlmInference")

        val generated = generate.invoke(engine, prompt) as? String
        generated?.takeIf { it.isNotBlank() }
            ?: error("Gemma 3n no devolvió contenido utilizable")
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
