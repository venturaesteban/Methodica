package com.methodica.app.domain.planning

import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.PlanningResult
import com.methodica.app.domain.model.PlanningSettings
import com.methodica.app.domain.model.PlanningStatus
import com.methodica.app.domain.model.StudySession
import com.methodica.app.domain.model.StudySessionStatus
import com.methodica.app.domain.model.StudySessionType
import com.methodica.app.domain.model.Topic
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil

/**
 * Motor de planificación puro: sin dependencias de Room, DataStore o Android.
 * Recibe datos ya cargados y devuelve un PlanningResult inmutable.
 */
class StudyPlanGenerator {

    fun generate(
        assessment:               Assessment,
        linkedTopics:             List<Topic>,
        settings:                 PlanningSettings,
        existingSessionsInWindow: List<StudySession>,
        today:                    LocalDate
    ): PlanningResult {

        val warnings = mutableListOf<String>()

        // --- Validaciones de entrada ---

        if (linkedTopics.isEmpty()) {
            return infeasibleResult("La evaluación no tiene temas asociados.")
        }

        val examDate = Instant.ofEpochMilli(assessment.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        if (!examDate.isAfter(today)) {
            return infeasibleResult("La fecha de la evaluación ya pasó o es hoy.")
        }

        if (settings.finalReviewDays > settings.bufferDaysBeforeExam) {
            return infeasibleResult("Los días de repaso final no pueden superar el colchón.")
        }

        // --- Calcular ventana de planificación [today, examDate) ---
        // examDate queda excluido

        val allDays = generateSequence(today) { it.plusDays(1) }
            .takeWhile { it.isBefore(examDate) }
            .toList()

        if (allDays.isEmpty()) {
            return infeasibleResult("No hay días disponibles entre hoy y la evaluación.")
        }

        // Separar zona protegida (tail) y zona de estudio
        val tailSize  = settings.bufferDaysBeforeExam.coerceAtMost(allDays.size)
        val studyDays = allDays.dropLast(tailSize)
        val tailDays  = allDays.takeLast(tailSize)

        // Dentro del tail, los últimos finalReviewDays son de repaso
        val reviewSize    = settings.finalReviewDays.coerceAtMost(tailDays.size)
        val reviewDays    = tailDays.takeLast(reviewSize)
        // Los días restantes del tail son colchón puro (no se asignan)

        // Filtrar días no disponibles por weekday
        val availableStudyDays  = studyDays.filterAvailable(settings.unavailableWeekdays)
        val availableReviewDays = reviewDays.filterAvailable(settings.unavailableWeekdays)

        // --- Mapa de capacidad diaria (descuenta sesiones existentes) ---

        val existingByDate = existingSessionsInWindow.groupBy {
            Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        fun capacityForDay(day: LocalDate): Int {
            val used = existingByDate[day]?.size ?: 0
            return (settings.availableHoursPerDay - used).coerceAtLeast(0)
        }

        // --- Calcular horas requeridas ---

        val topicBlocks = linkedTopics.sortedBy { it.order }.map { topic ->
            val multiplier = when (topic.difficulty) {
                1    -> 1.0
                2    -> 1.25
                3    -> 1.5
                else -> 1.0
            }
            val adjustedHours = ceil(topic.estimatedHours * multiplier).toInt()
            TopicBlock(topic, adjustedHours)
        }

        val totalStudyHoursNeeded  = topicBlocks.sumOf { it.hours }
        // Repaso: 1 bloque por tema solo si hay ventana de repaso configurada
        val totalReviewHoursNeeded = if (settings.finalReviewDays > 0) linkedTopics.size else 0

        // --- Distribuir bloques de estudio ---

        val sessions = mutableListOf<StudySession>()
        var remainingStudyBlocks = totalStudyHoursNeeded
        val studyCapacity = availableStudyDays.associateWith { capacityForDay(it) }.toMutableMap()

        for (block in topicBlocks) {
            var hoursLeft = block.hours
            for (day in availableStudyDays) {
                if (hoursLeft <= 0) break
                val cap = studyCapacity[day] ?: 0
                if (cap <= 0) continue
                val assign = minOf(hoursLeft, cap)
                repeat(assign) { idx ->
                    val position = settings.availableHoursPerDay - (studyCapacity[day] ?: 0) + idx
                    sessions.add(
                        StudySession(
                            subjectId       = assessment.subjectId,
                            assessmentId    = assessment.id,
                            topicId         = block.topic.id,
                            date            = day.toEpochMillis(),
                            positionInDay   = position,
                            type            = StudySessionType.STUDY,
                            status          = StudySessionStatus.PLANNED
                        )
                    )
                }
                studyCapacity[day] = cap - assign
                hoursLeft -= assign
                remainingStudyBlocks -= assign
            }
            if (hoursLeft > 0) {
                warnings.add("No se pudieron asignar todas las horas de \"${block.topic.name}\".")
            }
        }

        // --- Distribuir bloques de repaso ---

        var remainingReviewBlocks = totalReviewHoursNeeded
        val reviewCapacity = availableReviewDays.associateWith { capacityForDay(it) }.toMutableMap()

        for (day in availableReviewDays) {
            val cap = reviewCapacity[day] ?: 0
            if (cap <= 0 || remainingReviewBlocks <= 0) continue
            val assign = minOf(remainingReviewBlocks, cap)
            repeat(assign) { idx ->
                val position = settings.availableHoursPerDay - (reviewCapacity[day] ?: 0) + idx
                sessions.add(
                    StudySession(
                        subjectId       = assessment.subjectId,
                        assessmentId    = assessment.id,
                        topicId         = null,
                        date            = day.toEpochMillis(),
                        positionInDay   = position,
                        type            = StudySessionType.REVIEW,
                        status          = StudySessionStatus.PLANNED
                    )
                )
            }
            reviewCapacity[day] = cap - assign
            remainingReviewBlocks -= assign
        }

        if (remainingReviewBlocks > 0) {
            warnings.add("No se pudieron asignar todas las sesiones de repaso.")
        }

        // --- Clasificación ---

        val unscheduledStudy  = remainingStudyBlocks.coerceAtLeast(0)
        val unscheduledReview = remainingReviewBlocks.coerceAtLeast(0)
        val totalUnscheduled  = unscheduledStudy + unscheduledReview

        val status = when {
            totalUnscheduled > 0 -> PlanningStatus.INFEASIBLE
            else -> {
                val spareBlocks = studyCapacity.values.sum()
                val spareDays   = studyCapacity.count { it.value >= settings.availableHoursPerDay }
                if (spareDays >= 1 || spareBlocks >= 2) PlanningStatus.FEASIBLE
                else PlanningStatus.TIGHT
            }
        }

        return PlanningResult(
            sessions         = sessions,
            status           = status,
            totalStudyHours  = totalStudyHoursNeeded,
            totalReviewHours = totalReviewHoursNeeded,
            unscheduledHours = totalUnscheduled,
            warnings         = warnings
        )
    }

    // --- Helpers privados ---

    private data class TopicBlock(val topic: Topic, val hours: Int)

    private fun List<LocalDate>.filterAvailable(unavailable: Set<DayOfWeek>): List<LocalDate> =
        filter { it.dayOfWeek !in unavailable }

    private fun LocalDate.toEpochMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun infeasibleResult(warning: String): PlanningResult =
        PlanningResult(
            sessions         = emptyList(),
            status           = PlanningStatus.INFEASIBLE,
            totalStudyHours  = 0,
            totalReviewHours = 0,
            unscheduledHours = 0,
            warnings         = listOf(warning)
        )
}
