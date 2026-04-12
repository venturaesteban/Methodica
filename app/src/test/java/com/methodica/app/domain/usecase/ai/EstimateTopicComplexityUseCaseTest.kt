package com.methodica.app.domain.usecase.ai

import com.methodica.app.domain.ai.LlmProvider
import com.methodica.app.domain.model.AiProviderSettings
import com.methodica.app.domain.model.Degree
import com.methodica.app.domain.model.DegreeStatus
import com.methodica.app.domain.model.Material
import com.methodica.app.domain.model.MaterialType
import com.methodica.app.domain.model.PlanningSettings
import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.model.Topic
import com.methodica.app.domain.model.AcademicYear
import com.methodica.app.domain.repository.AiProviderSettingsRepository
import com.methodica.app.domain.repository.AcademicCatalogRepository
import com.methodica.app.domain.repository.MaterialRepository
import com.methodica.app.domain.repository.PlanningSettingsRepository
import com.methodica.app.domain.repository.SubjectRepository
import com.methodica.app.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EstimateTopicComplexityUseCaseTest {

    @Test
    fun `usa fallback heuristico y actualiza tema cuando IA externa no esta configurada`() = runTest {
        val topicRepository = FakeTopicRepository()
        val useCase = EstimateTopicComplexityUseCase(
            topicRepository = topicRepository,
            materialRepository = FakeMaterialRepository(),
            academicCatalogRepository = FakeAcademicCatalogRepository(),
            subjectRepository = FakeSubjectRepository(),
            planningSettingsRepository = FakePlanningSettingsRepository(),
            aiProviderSettingsRepository = FakeAiProviderSettingsRepository(),
            llmProvider = FakeLlmProvider()
        )

        val result = useCase(1L)

        assertTrue(result.exceptionOrNull()?.message, result.isSuccess)
        assertEquals("heuristic", result.getOrThrow().source)
        assertEquals(1, topicRepository.savedTopics.size)
        assertTrue(topicRepository.savedTopics.first().estimatedHours >= 3)
        assertTrue(topicRepository.savedTopics.first().difficulty in 1..3)
    }

    @Test
    fun `cuando IA externa devuelve dificultad y horas validas se persisten exactamente esos valores`() = runTest {
        val topicRepository = FakeTopicRepository()
        val useCase = EstimateTopicComplexityUseCase(
            topicRepository = topicRepository,
            materialRepository = FakeMaterialRepository(),
            academicCatalogRepository = FakeAcademicCatalogRepository(),
            subjectRepository = FakeSubjectRepository(),
            planningSettingsRepository = FakePlanningSettingsRepository(),
            aiProviderSettingsRepository = FakeEnabledAiProviderSettingsRepository(),
            llmProvider = FakeExternalAiLlmProvider(
                payload = "{\"difficulty\":3,\"estimated_hours\":7}"
            )
        )

        val result = useCase(1L)

        assertTrue(result.exceptionOrNull()?.message, result.isSuccess)
        assertEquals("external_ai", result.getOrThrow().source)
        assertEquals(3, result.getOrThrow().complexityScore)
        assertEquals(7, result.getOrThrow().estimatedHours)
        assertEquals(1, topicRepository.savedTopics.size)
        assertEquals(3, topicRepository.savedTopics.first().difficulty)
        assertEquals(7, topicRepository.savedTopics.first().estimatedHours)
    }

    @Test
    fun `cuando IA externa no devuelve json valido falla sin aplicar fallback heuristico`() = runTest {
        val topicRepository = FakeTopicRepository()
        val useCase = EstimateTopicComplexityUseCase(
            topicRepository = topicRepository,
            materialRepository = FakeMaterialRepository(),
            academicCatalogRepository = FakeAcademicCatalogRepository(),
            subjectRepository = FakeSubjectRepository(),
            planningSettingsRepository = FakePlanningSettingsRepository(),
            aiProviderSettingsRepository = FakeEnabledAiProviderSettingsRepository(),
            llmProvider = FakeExternalAiLlmProvider(payload = "respuesta no valida")
        )

        val result = useCase(1L)

        assertTrue(result.isFailure)
        assertTrue(topicRepository.savedTopics.isEmpty())
    }

    private class FakeTopicRepository : TopicRepository {
        val savedTopics = mutableListOf<Topic>()

        override fun observeTopics(subjectId: Long): Flow<List<Topic>> = flowOf(emptyList())

        override suspend fun getTopic(id: Long): Topic? = Topic(
            id = 1L,
            subjectId = 10L,
            name = "Tema Integrales",
            difficulty = 2,
            estimatedHours = 3,
            order = 0
        )

        override suspend fun saveTopic(topic: Topic) {
            savedTopics.add(topic)
        }

        override suspend fun deleteTopic(topic: Topic) = Unit
    }

    private class FakeMaterialRepository : MaterialRepository {
        override fun observeAllMaterials(): Flow<List<Material>> = flowOf(
            listOf(
                Material(
                    id = 1L,
                    subjectId = 10L,
                    topicId = 1L,
                    title = "Guía PDF",
                    uri = "content://docs/guia.pdf",
                    type = MaterialType.FILE_URI
                ),
                Material(
                    id = 2L,
                    subjectId = 10L,
                    topicId = 1L,
                    title = "Clase grabada",
                    uri = "https://example.com/clase.mp4",
                    type = MaterialType.VIDEO_LINK
                )
            )
        )

        override suspend fun getMaterial(id: Long): Material? = null

        override suspend fun saveMaterial(material: Material): Long = 1L

        override suspend fun deleteMaterial(material: Material) = Unit

        override suspend fun buildAiResourceSummary(material: Material, maxChars: Int): String? =
            "Resumen sintetico de ${material.title}"

        override suspend fun buildAiIndexableContent(material: Material, maxChars: Int): String? =
            "Contenido indexable de ${material.title}"
    }

    private class FakeSubjectRepository : SubjectRepository {
        override fun observeSubjects(): Flow<List<Subject>> = flowOf(
            listOf(
                Subject(
                    id = 100L,
                    name = "Álgebra",
                    colorHex = "#FF0000",
                    academicYearId = 1L,
                    degreeId = 5L,
                    degreeName = "Ingeniería",
                    courseYear = 1
                )
            )
        )

        override fun observeSubjectsByAcademicYearId(academicYearId: Long): Flow<List<Subject>> = flowOf(emptyList())

        override suspend fun getSubject(id: Long): Subject? = null

        override suspend fun saveSubject(subject: Subject) = Unit

        override suspend fun deleteSubject(subject: Subject) = Unit
    }

    private class FakePlanningSettingsRepository : PlanningSettingsRepository {
        override fun observeSettings(): Flow<PlanningSettings> = flowOf(
            PlanningSettings(
                studentAge = 21,
                studentCurrentDegreeIds = setOf(5L),
                readingComprehensionLevel = 3,
                passedSubjectIds = setOf(100L)
            )
        )

        override suspend fun saveSettings(settings: PlanningSettings) = Unit
    }

    private class FakeAiProviderSettingsRepository : AiProviderSettingsRepository {
        override fun observeSettings(): Flow<AiProviderSettings> = flowOf(
            AiProviderSettings(externalEnabled = false)
        )

        override suspend fun saveSettings(settings: AiProviderSettings) = Unit
    }

    private class FakeEnabledAiProviderSettingsRepository : AiProviderSettingsRepository {
        override fun observeSettings(): Flow<AiProviderSettings> = flowOf(
            AiProviderSettings(
                externalEnabled = true,
                providerName = "OpenAI",
                baseUrl = "https://api.openai.com/v1/chat/completions",
                model = "gpt-4o-mini",
                apiKey = "test-key"
            )
        )

        override suspend fun saveSettings(settings: AiProviderSettings) = Unit
    }

    private class FakeLlmProvider : LlmProvider {
        override suspend fun generate(
            baseUrl: String,
            model: String,
            apiKey: String,
            prompt: String
        ): String? = """
            {
              "complexity_score": 4,
              "estimated_hours": 6
            }
        """.trimIndent()
    }

    private class FakeExternalAiLlmProvider(
        private val payload: String
    ) : LlmProvider {
        override suspend fun generate(
            baseUrl: String,
            model: String,
            apiKey: String,
            prompt: String
        ): String? = payload
    }

    private class FakeAcademicCatalogRepository : AcademicCatalogRepository {
        override fun observeDegrees(): Flow<List<Degree>> = flowOf(
            listOf(Degree(id = 5L, name = "Ingeniería"))
        )

        override fun observeDegreesByStatus(status: DegreeStatus): Flow<List<Degree>> = flowOf(emptyList())

        override fun observeAcademicYears(degreeId: Long): Flow<List<AcademicYear>> = flowOf(emptyList())

        override suspend fun getDegree(id: Long): Degree? = null

        override suspend fun getAcademicYear(id: Long): AcademicYear? = null

        override suspend fun upsertDegree(degree: Degree): Long = 0L

        override suspend fun updateDegreeStatus(id: Long, status: DegreeStatus) = Unit

        override suspend fun deleteDegree(id: Long) = Unit

        override suspend fun deleteAcademicYear(id: Long) = Unit

        override suspend fun ensureDegree(name: String): Long = 0L

        override suspend fun ensureAcademicYear(degreeId: Long, yearNumber: Int): Long = 0L
    }
}
