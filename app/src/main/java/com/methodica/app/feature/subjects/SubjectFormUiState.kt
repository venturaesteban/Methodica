package com.methodica.app.feature.subjects

data class SubjectFormUiState(
    val name:        String  = "",
    val colorHex:    String  = "#6750A4",
    val description: String  = "",
    val isLoading:   Boolean = false,
    val isSaved:     Boolean = false,
    val nameError:   String? = null,
    val error:       String? = null
)
