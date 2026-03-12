package com.methodica.app.domain.planning

import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.AssessmentType
import com.methodica.app.domain.model.PlanningSettings
import com.methodica.app.domain.model.PlanningStatus
import com.methodica.app.domain.model.StudySession
import com.methodica.app.domain.model.StudySessionStatus
import com.methodica.app.domain.model.StudySessionType
import com.methodica.app.domain.model.Topic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class StudyPlanGeneratorTest {

    private val generator = StudyPlanGenerator()

    // — Helpers —

    private fun localDateToMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun defaultAssessment(daysFromNow: Long = 14): Assessment {
        val examDate = LocalDate.now().plusDays(daysFromNow)
        return Assessment(
            id        = 1L,
            subjectId = 10L,
            type      = AssessmentType.EXAM,
            title     = "Parcial 1",
            date      = localDateToMillis(examDate)
        )
    }

    private fun topic(id: Long, name: String, hours: Int, difficulty: Int = 1, order: Int = 0) =
        Topic(id = id, subjectId = 10L, name = name, difficulty = difficulty, estimatedHours = hours, order = order)

    private fun defaultSettings() = PlanningSettings(
        availableHoursPerDay = 4,
        unavailableWeekdays  = emptySet(),
        bufferDaysBeforeExam = 2,
        finalReviewDays      = 1
    )

    // — Tests: validaciones de entrada —

    @Test
    fun `INFEASIBLE cuando no hay topics asociados`() {
        val result = generator.generate(
            assessment               = defaultAssessment(),
            linkedTopics             = emptyList(),
            settings                 = defaultSettings(),
            existingSessionsInWindow = emptyList(),
            today                    = LocalDate.now()
        )
        assertEquals(PlanningStatus.INFEASIBLE, result.status)
        assertTrue(result.sessions.isEmpty())
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun `INFEASIBLE cuando la fecha del examen ya pasó`() {
        val result = generator.generate(
            assessment               = defaultAssessment(daysFromNow = -1),
            linkedTopics             = listOf(topic(1, "T1", 2)),
            settings                 = defaultSettings(),
            existingSessionsInWindow = emptyList(),
            today                    = LocalDate.now()
        )
        assertEquals(PlanningStatus.INFEASIBLE, result.status)
    }

    @Test
    fun `INFEASIBLE cuando la fecha del examen es hoy`() {
        val result = generator.generate(
            assessment               = defaultAssessment(daysFromNow = 0),
            linkedTopics             = listOf(topic(1, "T1", 2)),
            settings                 = defaultSettings(),
            existingSessionsInWindow = emptyList(),
            today                    = LocalDate.now()
        )
        assertEquals(PlanningStatus.INFEASIBLE, result.status)
    }

    @Test
    fun `INFEASIBLE cuando finalReviewDays supera bufferDays`() {
        val settings = defaultSettings().copy(bufferDaysBeforeExam = 1, finalReviewDays = 3)
        val result = generator.generate(
            assessment               = defaultAssessment(),
            linkedTopics             = listOf(topic(1, "T1", 2)),
            settings                 = settings,
            existingSessionsInWindow = emptyList(),
            today                    = LocalDate.now()
        )
        assertEquals(PlanningStatus.INFEASIBLE, result.status)
    }

    // — Tests: generación básica y clasificación —

    @Test
    fun `FEASIBLE con amplio margen`() {
        // 14 días, 4h/día, buffer=2, review=1, un topic de 2h
        val result = generator.generate(
            assessment               = defaultAssessment(daysFromNow = 14),
            linkedTopics             = listOf(topic(1, "T1", 2)),
            settings                 = defaultSettings(),
            existingSessionsInWindow = emptyList(),
            today                    = LocalDate.now()
        )
        assertEquals(PlanningStatus.FEASIBLE, result.status)
        assertEquals(0, result.unscheduledHours)
        assertTrue(result.sessions.isNotEmpty())
    }

    @Test
    fun `genera sesiones de STUDY y REVIEW`() {
        val result = generator.generate(
            assessment               = defaultAssessment(daysFromNow = 14),
            linkedTopics             = listOf(topic(1, "T1", 3)),
            settings                 = defaultSettings(),
            existingSessionsInWindow = emptyList(),
            today                    = LocalDate.now()
        )
        val studySessions  = result.sessions.filter { it.type == StudySessionType.STUDY }
        val reviewSessions = result.sessions.filter { it.type == StudySessionType.REVIEW }
        assertTrue(studySessions.isNotEmpty())
        assertTrue(reviewSessions.isNotEmpty())
    }

    @Test
    fun `no genera sesiones el día del examen`() {
        val today = LocalDate.now()
        val examDate = today.plusDays(14)
        val examMillis = localDateToMillis(examDate)
        val result = generator.generate(
            assessment               = defaultAssessment(daysFromNow = 14),
            linkedTopics             = listOf(topic(1, "T1", 2)),
            settings                 = defaultSettings(),
            existingSessionsInWindow = emptyList(),
            today                    = today
        )
        val examDaySessions = result.sessions.filter { it.date == examMillis }
        assertTrue(examDaySessions.isEmpty())
    }

    // — Tests: días no disponibles —

    @Test
    fun `nunca asigna sesiones en días no disponibles`() {
        val settings = defaultSettings().copy(unavailableWeekdays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        val result = generator.generate(
            assessment               = defaultAssessment(daysFromNow = 21),
            linkedTopics             = listOf(topic(1, "T1", 5)),
            settings                 = settings,
            existingSessionsInWindow = emptyList(),
            today                    = LocalDate.now()
        )
        val zone = ZoneId.systemDefault()
        result.sessions.forEach { session ->
            val dayOfWeek = java.time.Instant.ofEpochMilli(session.date).atZone(zone).toLocalDate().dayOfWeek
            assertTrue("Sesión asignada en día no disponible: $dayOfWeek", dayOfWeek !in settings.unavailableWeekdays)
        }
    }

    // — Tests: buffer y repaso —

    @Test
    fun `no crea sesiones de estudio dentro del buffer`() {
        val today = LocalDate.now()
        val examDate = today.plusDays(10)
        val settings = defaultSettings().copy(bufferDaysBeforeExam = 3, finalReviewDays = 1)
        val result = generator.generate(
            assessment               = Assessment(1L, 10L, AssessmentType.EXAM, "P1", localDateToMillis(examDate)),
            linkedTopics             = listOf(topic(1, "T1", 4)),
            settings                 = settings,
            existingSessionsInWindow = emptyList(),
            today                    = today
        )
        // Los últimos 3 días antes del examen son buffer
        val bufferStart = examDate.minusDays(3)
        val studySessions = result.sessions.filter { it.type == StudySessionType.STUDY }
        val zone = ZoneId.systemDefault()
        studySessions.forEach { session ->
            val sessionDate = java.time.Instant.ofEpochMilli(session.date).atZone(zone).toLocalDate()
            assertTrue(
                "Sesión de estudio en zona protegida: $sessionDate >= $bufferStart",
                sessionDate.isBefore(bufferStart)
            )
        }
    }

    @Test
    fun `las sesiones de repaso están en los últimos finalReviewDays del buffer`() {
        val today = LocalDate.now()
        val examDate = today.plusDays(10)
        val settings = defaultSettings().copy(bufferDaysBeforeExam = 3, finalReviewDays = 2)
        val result = generator.generate(
            assessment               = Assessment(1L, 10L, AssessmentType.EXAM, "P1", localDateToMillis(examDate)),
            linkedTopics             = listOf(topic(1, "T1", 4)),
            settings                 = settings,
            existingSessionsInWindow = emptyList(),
            today                    = today
        )
        val zone = ZoneId.systemDefault()
        // Review days: los 2 últimos del tail de 3 → [examDate-2, examDate-1]
        val reviewSessions = result.sessions.filter { it.type == StudySessionType.REVIEW }
        reviewSessions.forEach { session ->
            val sessionDate = java.time.Instant.ofEpochMilli(session.date).atZone(zone).toLocalDate()
            assertTrue(
                "Sesión de repaso fuera de zona de repaso: $sessionDate",
                !sessionDate.isBefore(examDate.minusDays(2)) && sessionDate.isBefore(examDate)
            )
        }
    }

    // — Tests: dificultad —

    @Test
    fun `dificultad aplica multiplicador correctamente`() {
        // Topic con 4h estimadas y dificultad 3 → ceil(4*1.5) = 6h
        val result = generator.generate(
            assessment               = defaultAssessment(daysFromNow = 14),
            linkedTopics             = listOf(topic(1, "T1", 4, difficulty = 3)),
            settings                 = defaultSettings(),
            existingSessionsInWindow = emptyList(),
            today                    = LocalDate.now()
        )
        assertEquals(6, result.totalStudyHours)
    }

    @Test
    fun `dificultad 1 no altera horas`() {
        val result = generator.generate(
            assessment               = defaultAssessment(daysFromNow = 14),
            linkedTopics             = listOf(topic(1, "T1", 3, difficulty = 1)),
            settings                 = defaultSettings(),
            existingSessionsInWindow = emptyList(),
            today                    = LocalDate.now()
        )
        assertEquals(3, result.totalStudyHours)
    }

    @Test
    fun `dificultad 2 aplica x1_25 con redondeo`() {
        // 3h * 1.25 = 3.75 → ceil = 4
        val result = generator.generate(
            assessment               = defaultAssessment(daysFromNow = 14),
            linkedTopics             = listOf(topic(1, "T1", 3, difficulty = 2)),
            settings                 = defaultSettings(),
            existingSessionsInWindow = emptyList(),
            today                    = LocalDate.now()
        )
        assertEquals(4, result.totalStudyHours)
    }

    // — Tests: clasificación —

    @Test
    fun `TIGHT cuando todo cabe pero sin margen`() {
        // 3 días, 2h/día, buffer=0, review=0
        // 1 topic de 6h → necesita exactamente 6 bloques en 3 días × 2h = 6 bloques
        val today = LocalDate.now()
        val examDate = today.plusDays(3)
        val settings = PlanningSettings(
            availableHoursPerDay = 2,
            unavailableWeekdays  = emptySet(),
            bufferDaysBeforeExam = 0,
            finalReviewDays      = 0
        )
        val result = generator.generate(
            assessment               = Assessment(1L, 10L, AssessmentType.EXAM, "P1", localDateToMillis(examDate)),
            linkedTopics             = listOf(topic(1, "T1", 6)),
            settings                 = settings,
            existingSessionsInWindow = emptyList(),
            today                    = today
        )
        assertEquals(PlanningStatus.TIGHT, result.status)
        assertEquals(0, result.unscheduledHours)
    }

    @Test
    fun `INFEASIBLE cuando no hay suficiente capacidad`() {
        // 2 días, 1h/día, buffer=0, review=0 → solo 2 bloques para 5h
        val today = LocalDate.now()
        val examDate = today.plusDays(2)
        val settings = PlanningSettings(
            availableHoursPerDay = 1,
            unavailableWeekdays  = emptySet(),
            bufferDaysBeforeExam = 0,
            finalReviewDays      = 0
        )
        val result = generator.generate(
            assessment               = Assessment(1L, 10L, AssessmentType.EXAM, "P1", localDateToMillis(examDate)),
            linkedTopics             = listOf(topic(1, "T1", 5)),
            settings                 = settings,
            existingSessionsInWindow = emptyList(),
            today                    = today
        )
        assertEquals(PlanningStatus.INFEASIBLE, result.status)
        assertTrue(result.unscheduledHours > 0)
    }

    // — Tests: sesiones existentes descuentan capacidad —

    @Test
    fun `descuenta sesiones existentes de la capacidad diaria`() {
        val today = LocalDate.now()
        val examDate = today.plusDays(3)
        val settings = PlanningSettings(
            availableHoursPerDay = 2,
            unavailableWeekdays  = emptySet(),
            bufferDaysBeforeExam = 0,
            finalReviewDays      = 0
        )
        // Ya hay 1 sesión cada día → solo queda 1h/día = 3h total
        val existing = listOf(
            StudySession(id = 100, subjectId = 99, assessmentId = 99, topicId = null,
                date = localDateToMillis(today), positionInDay = 0, type = StudySessionType.STUDY),
            StudySession(id = 101, subjectId = 99, assessmentId = 99, topicId = null,
                date = localDateToMillis(today.plusDays(1)), positionInDay = 0, type = StudySessionType.STUDY),
            StudySession(id = 102, subjectId = 99, assessmentId = 99, topicId = null,
                date = localDateToMillis(today.plusDays(2)), positionInDay = 0, type = StudySessionType.STUDY)
        )
        val result = generator.generate(
            assessment               = Assessment(1L, 10L, AssessmentType.EXAM, "P1", localDateToMillis(examDate)),
            linkedTopics             = listOf(topic(1, "T1", 4)),
            settings                 = settings,
            existingSessionsInWindow = existing,
            today                    = today
        )
        // Solo caben 3 bloques de estudio, se necesitan 4 → INFEASIBLE
        assertEquals(PlanningStatus.INFEASIBLE, result.status)
    }

    // — Tests: múltiples topics respetan orden —

    @Test
    fun `sesiones de estudio siguen order de topics`() {
        val result = generator.generate(
            assessment               = defaultAssessment(daysFromNow = 14),
            linkedTopics             = listOf(
                topic(1, "Tema A", 2, order = 0),
                topic(2, "Tema B", 2, order = 1),
                topic(3, "Tema C", 2, order = 2)
            ),
            settings                 = defaultSettings(),
            existingSessionsInWindow = emptyList(),
            today                    = LocalDate.now()
        )
        val studySessions = result.sessions.filter { it.type == StudySessionType.STUDY }
        val topicOrder = studySessions.map { it.topicId }
        // Primer bloque debe ser topic 1, luego 2, luego 3
        val firstTopic1 = topicOrder.indexOfFirst { it == 1L }
        val firstTopic2 = topicOrder.indexOfFirst { it == 2L }
        val firstTopic3 = topicOrder.indexOfFirst { it == 3L }
        assertTrue(firstTopic1 < firstTopic2)
        assertTrue(firstTopic2 < firstTopic3)
    }

    // — Tests: todas las sesiones son PLANNED y autoGenerated —

    @Test
    fun `todas las sesiones generadas son PLANNED y autoGenerated`() {
        val result = generator.generate(
            assessment               = defaultAssessment(daysFromNow = 14),
            linkedTopics             = listOf(topic(1, "T1", 3)),
            settings                 = defaultSettings(),
            existingSessionsInWindow = emptyList(),
            today                    = LocalDate.now()
        )
        result.sessions.forEach { session ->
            assertEquals(StudySessionStatus.PLANNED, session.status)
            assertTrue(session.isAutoGenerated)
        }
    }

    // — Tests: review hours count —

    @Test
    fun `totalReviewHours es igual al número de topics`() {
        val topics = listOf(
            topic(1, "T1", 2, order = 0),
            topic(2, "T2", 3, order = 1)
        )
        val result = generator.generate(
            assessment               = defaultAssessment(daysFromNow = 14),
            linkedTopics             = topics,
            settings                 = defaultSettings(),
            existingSessionsInWindow = emptyList(),
            today                    = LocalDate.now()
        )
        assertEquals(topics.size, result.totalReviewHours)
    }
}
