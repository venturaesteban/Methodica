package com.methodica.app.feature.subjects

import com.methodica.app.domain.model.Subject

data class SubjectsUiState(
    val subjects:  List<Subject> = emptyList(),
    val isLoading: Boolean       = false,
    val error:     String?       = null
)

