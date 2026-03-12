package com.methodica.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicTest {

    @Test
    fun `nuevo topic tiene id cero por defecto`() {
        val topic = Topic(subjectId = 1L, name = "Álgebra lineal")
        assertEquals(0L, topic.id)
    }

    @Test
    fun `difficulty por defecto es 1`() {
        val topic = Topic(subjectId = 1L, name = "Álgebra lineal")
        assertEquals(1, topic.difficulty)
    }

    @Test
    fun `estimatedHours por defecto es 1`() {
        val topic = Topic(subjectId = 1L, name = "Álgebra lineal")
        assertEquals(1, topic.estimatedHours)
    }

    @Test
    fun `order por defecto es 0`() {
        val topic = Topic(subjectId = 1L, name = "Álgebra lineal")
        assertEquals(0, topic.order)
    }

    @Test
    fun `topic mantiene sus propiedades sin mutarlas`() {
        val topic = Topic(id = 5L, subjectId = 2L, name = "Derivadas", difficulty = 3, estimatedHours = 4, order = 1)
        assertEquals(5L, topic.id)
        assertEquals(2L, topic.subjectId)
        assertEquals("Derivadas", topic.name)
        assertEquals(3, topic.difficulty)
        assertEquals(4, topic.estimatedHours)
        assertEquals(1, topic.order)
    }

    @Test
    fun `dos topics con el mismo contenido son iguales por data class`() {
        val a = Topic(id = 1L, subjectId = 1L, name = "Integrales")
        val b = Topic(id = 1L, subjectId = 1L, name = "Integrales")
        assertEquals(a, b)
    }
}
