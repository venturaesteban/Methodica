package com.methodica.app.feature.degree

import com.methodica.app.domain.model.Degree

data class DegreesUiState(
    val degrees: List<Degree> = emptyList(),
    val isLoading: Boolean = true
)

