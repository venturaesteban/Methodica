package com.methodica.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssessmentTest {

    @Test
    fun `nuevo assessment tiene id cero por defecto`() {
        val assessment = Assessment(subjectId = 1L, type = AssessmentType.EXAM, title = "Parcial 1", date = 1_000_000L)
        assertEquals(0L, assessment.id)
    }

    @Test
    fun `weight y notes son nulos por defecto`() {
        val assessment = Assessment(subjectId = 1L, type = AssessmentType.QUIZ, title = "Quiz 1", date = 1_000_000L)
        assertNull(assessment.weight)
        assertNull(assessment.notes)
    }

    @Test
    fun `assessment mantiene sus propiedades sin mutarlas`() {
        val assessment = Assessment(
            id        = 3L,
            subjectId = 2L,
            type      = AssessmentType.ASSIGNMENT,
            title     = "Tarea 2",
            date      = 1_700_000_000L,
            weight    = 20,
            notes     = "Entregar en PDF"
        )
        assertEquals(3L, assessment.id)
        assertEquals(2L, assessment.subjectId)
        assertEquals(AssessmentType.ASSIGNMENT, assessment.type)
        assertEquals("Tarea 2", assessment.title)
        assertEquals(1_700_000_000L, assessment.date)
        assertEquals(20, assessment.weight)
        assertEquals("Entregar en PDF", assessment.notes)
    }

    @Test
    fun `AssessmentType fromName devuelve el tipo correcto`() {
        assertEquals(AssessmentType.EXAM,       AssessmentType.fromName("EXAM"))
        assertEquals(AssessmentType.QUIZ,       AssessmentType.fromName("QUIZ"))
        assertEquals(AssessmentType.ASSIGNMENT, AssessmentType.fromName("ASSIGNMENT"))
        assertEquals(AssessmentType.PROJECT,    AssessmentType.fromName("PROJECT"))
    }

    @Test
    fun `AssessmentType fromName desconocido retorna EXAM por defecto`() {
        assertEquals(AssessmentType.EXAM, AssessmentType.fromName("UNKNOWN"))
    }
}
