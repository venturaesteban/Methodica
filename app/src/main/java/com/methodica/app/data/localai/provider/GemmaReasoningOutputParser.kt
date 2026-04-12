package com.methodica.app.data.localai.provider

import com.methodica.app.domain.ai.local.ReasoningPlanOutput
import com.methodica.app.domain.ai.local.ReasoningScope
import com.methodica.app.domain.ai.local.ReasoningSequencingStep
import com.methodica.app.domain.ai.local.ReasoningTopicComplexity
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class GemmaReasoningOutputParser @Inject constructor() {

    fun parse(rawOutput: String): Result<ReasoningPlanOutput> = runCatching {
        val json = JSONObject(extractJson(rawOutput))

        val scopeObj = json.getJSONObject("scope")
        val scope = ReasoningScope(
            estimatedScope = scopeObj.getString("estimatedScope").trim(),
            justification = scopeObj.getString("justification").trim(),
            confidence = scopeObj.optDouble("confidence", 0.35).toFloat().coerceIn(0f, 1f),
            requiresUserConfirmation = scopeObj.optBoolean("requiresUserConfirmation", true)
        )

        val topics = json.getJSONArray("topicComplexities").let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        ReasoningTopicComplexity(
                            topicName = item.getString("topicName").trim(),
                            complexityLevel = item.optInt("complexityLevel", 3).coerceIn(1, 5),
                            recommendedHours = item.optInt("recommendedHours", 2).coerceAtLeast(1),
                            priority = item.optInt("priority", 3).coerceIn(1, 5),
                            isIncludedInScope = item.optBoolean("isIncludedInScope", true),
                            requiresPractice = item.optBoolean("requiresPractice", true),
                            requiresSpacedReview = item.optBoolean("requiresSpacedReview", false),
                            rationale = item.optString("rationale", "Sin justificación").trim()
                        )
                    )
                }
            }
        }

        val sequencing = json.optJSONArray("sequencing")?.let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        ReasoningSequencingStep(
                            order = item.optInt("order", i + 1).coerceAtLeast(1),
                            topicName = item.optString("topicName").trim(),
                            why = item.optString("why").trim()
                        )
                    )
                }
            }
        }.orEmpty().filter { it.topicName.isNotBlank() }

        val risks = json.optJSONArray("risks")?.let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    val risk = array.optString(i).trim()
                    if (risk.isNotBlank()) add(risk)
                }
            }
        }.orEmpty()

        val output = ReasoningPlanOutput(
            scope = scope,
            topicComplexities = topics,
            sequencing = sequencing.sortedBy { it.order },
            risks = risks,
            summary = json.optString("summary", "").ifBlank { scope.justification.take(220) },
            confidence = json.optDouble("confidence", scope.confidence.toDouble()).toFloat().coerceIn(0f, 1f)
        )
        validate(output)
        output
    }

    private fun validate(output: ReasoningPlanOutput) {
        require(output.scope.estimatedScope.isNotBlank()) { "scope.estimatedScope vacío" }
        require(output.scope.justification.isNotBlank()) { "scope.justification vacío" }
        require(output.topicComplexities.isNotEmpty()) { "topicComplexities vacío" }
        require(output.topicComplexities.any { it.isIncludedInScope }) { "No hay temas incluidos en alcance" }
        require(output.summary.isNotBlank()) { "summary vacío" }
    }

    private fun extractJson(rawOutput: String): String {
        val cleaned = rawOutput
            .replace("```json", "")
            .replace("```", "")
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        require(start >= 0 && end > start) { "No se encontró JSON en la salida del modelo" }
        return cleaned.substring(start, end + 1)
    }
}
