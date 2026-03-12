package com.methodica.app.domain.model

/**
 * Modelo de dominio para una evaluación académica.
 * date: milisegundos desde epoch (almacenado como Long; sin TypeConverter en Room).
 * weight: porcentaje (0..100) opcional.
 */
data class Assessment(
    val id:        Long            = 0,
    val subjectId: Long,
    val type:      AssessmentType,
    val title:     String,
    val date:      Long,
    val weight:    Int?            = null,
    val notes:     String?         = null
)
