package com.methodica.app.domain.model

/**
 * Modelo de dominio de una materia académica.
 * Circula exclusivamente entre la capa domain y las features;
 * nunca se expone SubjectEntity fuera de la capa data.
 */
data class Subject(
    val id:          Long    = 0,
    val name:        String,
    val colorHex:    String,
    val description: String? = null
)
