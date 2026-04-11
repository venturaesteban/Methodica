package com.methodica.app.domain.usecase.ai

import com.methodica.app.domain.ai.LlmProvider
import com.methodica.app.domain.model.AiProviderSettings
import com.methodica.app.domain.model.Material
import com.methodica.app.domain.model.MaterialType
import com.methodica.app.domain.model.PlanningSettings
import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.model.TopicComplexityEstimate
import com.methodica.app.domain.repository.AiProviderSettingsRepository
import com.methodica.app.domain.repository.AcademicCatalogRepository
import com.methodica.app.domain.repository.MaterialRepository
import com.methodica.app.domain.repository.PlanningSettingsRepository
import com.methodica.app.domain.repository.SubjectRepository
import com.methodica.app.domain.repository.TopicRepository
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import org.json.JSONObject

class EstimateTopicComplexityUseCase(
    private val topicRepository: TopicRepository,
    private val materialRepository: MaterialRepository,
    private val academicCatalogRepository: AcademicCatalogRepository,
    private val subjectRepository: SubjectRepository,
    private val planningSettingsRepository: PlanningSettingsRepository,
    private val aiProviderSettingsRepository: AiProviderSettingsRepository,
    private val llmProvider: LlmProvider
) {

    suspend operator fun invoke(topicId: Long): Result<TopicComplexityEstimate> = runCatching {
        val topic = topicRepository.getTopic(topicId) ?: error("Tema no encontrado")
        val planningSettings = planningSettingsRepository.observeSettings().first()
        val subjectTopics = topicRepository.observeTopics(topic.subjectId).first()
            .sortedBy { it.order }
        val materials = materialRepository.observeAllMaterials().first()
            .filter { material -> material.topicId == topic.id && material.subjectId == topic.subjectId }
            .take(8)

        val aiSettings = aiProviderSettingsRepository.observeSettings().first()
        val estimate = if (aiSettings.isEnabledAndConfigured) {
            estimateWithExternalAi(
                topic = topic,
                subjectTopics = subjectTopics,
                materials = materials,
                planningSettings = planningSettings,
                aiSettings = aiSettings
            ) ?: error("La IA no devolvio una estimacion valida para complejidad/horas")
        } else {
            estimateHeuristically(topic, materials)
        }

        topicRepository.saveTopic(
            topic.copy(
                difficulty = estimate.complexityScore,
                estimatedHours = estimate.estimatedHours
            )
        )

        estimate
    }

    private suspend fun estimateWithExternalAi(
        topic: Topic,
        subjectTopics: List<Topic>,
        materials: List<Material>,
        planningSettings: PlanningSettings,
        aiSettings: AiProviderSettings
    ): TopicComplexityEstimate? {
        val subjects = subjectRepository.observeSubjects().first()
        val topicSubject = subjects.firstOrNull { it.id == topic.subjectId }
        val degrees = academicCatalogRepository.observeDegrees().first()
        val degreeNameById = degrees.associate { it.id to it.name }
        val passedSubjects = subjects
            .filter { it.id in planningSettings.passedSubjectIds }
            .take(25)
            .joinToString { "${it.name} (C${it.courseYear})" }
            .ifBlank { "Ninguna" }
        val currentDegreeIds = planningSettings.studentCurrentDegreeIds
        val currentDegrees = planningSettings.studentCurrentDegreeIds
            .mapNotNull { degreeId -> degreeNameById[degreeId] }
            .distinct()
            .joinToString()
            .ifBlank { "Ninguna" }
        val completedDegrees = planningSettings.studentCompletedDegreeIds
            .mapNotNull { degreeId -> degreeNameById[degreeId] }
            .distinct()
            .joinToString()
            .ifBlank { "Ninguna" }
        val allCurrentDegreeSubjects = if (currentDegreeIds.isEmpty()) {
            subjects
        } else {
            subjects.filter { it.degreeId in currentDegreeIds }
        }
        val passedInCurrentDegrees = allCurrentDegreeSubjects.count { it.id in planningSettings.passedSubjectIds }
        val totalCurrentDegreeSubjects = allCurrentDegreeSubjects.size
        val completionRatioText = if (totalCurrentDegreeSubjects == 0) {
            "0%"
        } else {
            "${(passedInCurrentDegrees * 100) / totalCurrentDegreeSubjects}%"
        }
        val siblingTopicsSummary = subjectTopics
            .filter { it.id != topic.id }
            .take(15)
            .joinToString(separator = "\n") {
                "- ${it.name}: dif=${it.difficulty}, horas=${it.estimatedHours}"
            }
            .ifBlank { "- No hay otros temas en la asignatura" }
        val unavailableDaysText = planningSettings.unavailableWeekdays
            .sortedBy { it.value }
            .joinToString { day -> day.toSpanishLabel() }
            .ifBlank { "Ninguno" }

        val resourcesSummary = summarizeMaterialsForAi(materials)
        val prompt = """
            Estima complejidad y horas de estudio para este tema academico.
            Responde SOLO JSON válido, sin markdown, sin texto adicional.

            JSON esperado:
            {
              "difficulty": <entero 1..3>,
              "estimated_hours": <entero >= 1>
            }

            Tema: ${topic.name}
            Asignatura: ${topicSubject?.name ?: "No disponible"}
            Curso academico: ${topicSubject?.courseYear ?: "No disponible"}
            Titulacion de la asignatura: ${topicSubject?.degreeName ?: "No disponible"}
            Posicion del tema en la asignatura: ${topic.order + 1}/${subjectTopics.size.coerceAtLeast(1)}

            Estado actual del tema:
            Dificultad actual (1..3): ${topic.difficulty}
            Horas actuales: ${topic.estimatedHours}

            Perfil del estudiante:
            - Edad: ${planningSettings.studentAge}
            - Titulaciones en curso: $currentDegrees
            - Titulaciones aprobadas: $completedDegrees
            - Comprensión lectora (1..5): ${planningSettings.readingComprehensionLevel}
            - Asignaturas aprobadas: $passedSubjects
            - Progreso en asignaturas de titulaciones en curso: $passedInCurrentDegrees/$totalCurrentDegreeSubjects ($completionRatioText)

            Preferencias de planificacion del estudiante:
            - Horas disponibles por dia: ${planningSettings.availableHoursPerDay}
            - Dias no disponibles: $unavailableDaysText
            - Colchon previo a examen (dias): ${planningSettings.bufferDaysBeforeExam}
            - Dias de repaso final: ${planningSettings.finalReviewDays}

            Temas relacionados de la misma asignatura:
            $siblingTopicsSummary

            Recursos del tema (resumen para analisis):
            $resourcesSummary

            REGLAS:
            - Ajusta dificultad y horas segun el perfil y experiencia previa del estudiante.
            - Si hay alta experiencia previa en el area, evita sobreestimar horas.
            - Si hay baja experiencia o baja comprension lectora, incrementa horas si procede.
            - Devuelve solo el JSON pedido.
        """.trimIndent()

        val raw = llmProvider.generate(
            baseUrl = aiSettings.baseUrl,
            model = aiSettings.model,
            apiKey = aiSettings.apiKey,
            prompt = prompt
        ) ?: return null

        return parseEstimateJson(raw)
            ?: error("Respuesta IA invalida para complejidad/horas: ${raw.take(250)}")
    }

    private fun parseEstimateJson(raw: String): TopicComplexityEstimate? {
        val fencedJson = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)

        val jsonCandidates = listOfNotNull(
            raw.trim(),
            fencedJson?.trim(),
            Regex("\\{[\\s\\S]*\\}").find(raw)?.value
        ).distinct()

        fun parseCandidate(candidate: String): TopicComplexityEstimate? = runCatching {
            val root = JSONObject(candidate)
            val directDifficulty = root.optInt("difficulty", -1)
            val difficultyFromCompatibilityScore = when (root.optInt("complexity_score", -1)) {
                1, 2 -> 1
                3 -> 2
                4, 5 -> 3
                else -> -1
            }
            val score = if (directDifficulty in 1..3) directDifficulty else difficultyFromCompatibilityScore
            val hours = root.optInt("estimated_hours", -1)
            if (score !in 1..3 || hours < 1) return null
            TopicComplexityEstimate(
                complexityScore = score,
                estimatedHours = hours,
                source = "external_ai"
            )
        }.getOrNull()

        val jsonParsed = jsonCandidates.firstNotNullOfOrNull { candidate -> parseCandidate(candidate) }
        if (jsonParsed != null) return jsonParsed

        val rawDifficulty = Regex("\"difficulty\"\\s*:\\s*(\\d+)", RegexOption.IGNORE_CASE)
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        val rawCompatibilityScore = Regex("\"complexity_score\"\\s*:\\s*(\\d+)", RegexOption.IGNORE_CASE)
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        val resolvedDifficulty = when {
            rawDifficulty in 1..3 -> rawDifficulty ?: -1
            rawCompatibilityScore in 1..2 -> 1
            rawCompatibilityScore == 3 -> 2
            rawCompatibilityScore in 4..5 -> 3
            else -> -1
        }
        val rawHours = Regex("\"estimated_hours\"\\s*:\\s*(\\d+)", RegexOption.IGNORE_CASE)
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        if (resolvedDifficulty !in 1..3 || rawHours == null || rawHours < 1) return null
        return TopicComplexityEstimate(
            complexityScore = resolvedDifficulty,
            estimatedHours = rawHours,
            source = "external_ai"
        )
    }

    private fun estimateHeuristically(
        topic: Topic,
        materials: List<Material>
    ): TopicComplexityEstimate {
        val totalEstimatedMinutes: Int = materials.fold(0) { acc, material ->
            acc + when {
                material.type == MaterialType.VIDEO_LINK -> 45
                material.uri.endsWith(".mp3", ignoreCase = true) ||
                    material.uri.endsWith(".wav", ignoreCase = true) ||
                    material.uri.endsWith(".m4a", ignoreCase = true) -> 35
                material.uri.endsWith(".pdf", ignoreCase = true) -> 60
                material.uri.endsWith(".ppt", ignoreCase = true) ||
                    material.uri.endsWith(".pptx", ignoreCase = true) -> 50
                material.type == MaterialType.WEB_LINK -> 30
                else -> 40
            }
        }

        val metadataComplexityBonus = when {
            totalEstimatedMinutes >= 420 -> 2
            totalEstimatedMinutes >= 180 -> 1
            else -> 0
        }

        val complexityScore = (topic.difficulty + metadataComplexityBonus).coerceIn(1, 3)
        val estimatedHours = ((topic.estimatedHours.coerceAtLeast(1) + (totalEstimatedMinutes / 60.0)).toInt())
            .coerceIn(1, 120)

        return TopicComplexityEstimate(
            complexityScore = complexityScore,
            estimatedHours = estimatedHours,
            source = "heuristic"
        )
    }

    private fun summarizeMaterials(materials: List<Material>): String {
        if (materials.isEmpty()) return "- Sin recursos asociados"
        return materials.joinToString(separator = "\n") { material ->
            val kind = when (material.type) {
                MaterialType.FILE_URI -> "archivo"
                MaterialType.WEB_LINK -> "enlace"
                MaterialType.VIDEO_LINK -> "vídeo"
            }
            val sizeHint = when {
                material.uri.endsWith(".pdf", ignoreCase = true) -> "pdf (lectura)"
                material.uri.endsWith(".mp3", ignoreCase = true) ||
                    material.uri.endsWith(".wav", ignoreCase = true) ||
                    material.uri.endsWith(".m4a", ignoreCase = true) -> "audio"
                material.uri.endsWith(".mp4", ignoreCase = true) ||
                    material.uri.endsWith(".mkv", ignoreCase = true) -> "video"
                else -> "metadatos básicos"
            }
            "- ${material.title}: $kind, $sizeHint"
        }
    }

    private suspend fun summarizeMaterialsForAi(materials: List<Material>): String {
        if (materials.isEmpty()) return "- Sin recursos asociados"

        return materials.take(6).mapIndexed { index, material ->
            val aiSummary = materialRepository.buildAiResourceSummary(
                material = material,
                maxChars = 1400
            )
            val fallback = summarizeMaterials(listOf(material)).removePrefix("- ")
            val resolved = aiSummary?.takeIf { it.isNotBlank() } ?: fallback
            "- Recurso ${index + 1} (${material.title}): $resolved"
        }.joinToString(separator = "\n")
    }

    private fun DayOfWeek.toSpanishLabel(): String = when (this) {
        DayOfWeek.MONDAY -> "Lunes"
        DayOfWeek.TUESDAY -> "Martes"
        DayOfWeek.WEDNESDAY -> "Miercoles"
        DayOfWeek.THURSDAY -> "Jueves"
        DayOfWeek.FRIDAY -> "Viernes"
        DayOfWeek.SATURDAY -> "Sabado"
        DayOfWeek.SUNDAY -> "Domingo"
    }
}
