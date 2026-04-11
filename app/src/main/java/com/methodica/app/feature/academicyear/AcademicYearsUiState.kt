package com.methodica.app.feature.academicyear

import com.methodica.app.domain.model.AcademicYear
import com.methodica.app.domain.model.Degree

data class AcademicYearsUiState(
    val degree: Degree? = null,
    val academicYears: List<AcademicYear> = emptyList(),
    val isLoading: Boolean = true
)

