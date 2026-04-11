package com.methodica.app.domain.model

data class Degree(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val status: DegreeStatus = DegreeStatus.ACTIVE
)

