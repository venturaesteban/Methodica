package com.methodica.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubjectTest {

    @Test
    fun `nuevo subject tiene id cero por defecto`() {
        val subject = Subject(name = "Matemáticas", colorHex = "#FF5722")
        assertEquals(0L, subject.id)
    }

    @Test
    fun `description es nula por defecto`() {
        val subject = Subject(name = "Historia", colorHex = "#2196F3")
        assertNull(subject.description)
    }

    @Test
    fun `subject mantiene sus propiedades sin mutarlas`() {
        val subject = Subject(id = 7L, name = "Historia", colorHex = "#2196F3", description = "Test")
        assertEquals(7L, subject.id)
        assertEquals("Historia", subject.name)
        assertEquals("#2196F3", subject.colorHex)
        assertEquals("Test", subject.description)
    }

    @Test
    fun `dos subjects con el mismo id son iguales por data class`() {
        val a = Subject(id = 1L, name = "Física", colorHex = "#4CAF50")
        val b = Subject(id = 1L, name = "Física", colorHex = "#4CAF50")
        assertEquals(a, b)
    }
}

