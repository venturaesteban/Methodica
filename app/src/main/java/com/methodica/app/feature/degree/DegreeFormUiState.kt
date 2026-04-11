package com.methodica.app.feature.degree

import com.methodica.app.domain.model.Degree

data class DegreeFormUiState(
    val degree: Degree? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

