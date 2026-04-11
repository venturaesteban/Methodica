package com.methodica.app.domain.model

data class AcademicYear(
    val id: Long = 0,
    val degreeId: Long,
    val yearNumber: Int,
    val name: String
)

