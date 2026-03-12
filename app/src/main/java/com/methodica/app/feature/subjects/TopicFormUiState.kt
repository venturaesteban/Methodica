package com.methodica.app.feature.subjects

data class TopicFormUiState(
    val name:                String  = "",
    val difficulty:          Int     = 1,
    val estimatedHours:      Int     = 1,
    val order:               Int     = 0,
    val isLoading:           Boolean = false,
    val isSaved:             Boolean = false,
    val nameError:           String? = null,
    val estimatedHoursError: String? = null,
    val error:               String? = null
)
