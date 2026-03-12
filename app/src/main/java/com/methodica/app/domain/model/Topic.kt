package com.methodica.app.domain.model

/**
 * Modelo de dominio para un tema dentro de una materia.
 * difficulty: 1 = fácil, 2 = medio, 3 = difícil.
 * estimatedHours: horas de estudio estimadas, debe ser > 0.
 * order: posición dentro de la materia, >= 0.
 */
data class Topic(
    val id:             Long   = 0,
    val subjectId:      Long,
    val name:           String,
    val difficulty:     Int    = 1,
    val estimatedHours: Int    = 1,
    val order:          Int    = 0
)
