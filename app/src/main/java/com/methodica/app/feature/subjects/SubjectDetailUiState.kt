package com.methodica.app.feature.subjects

import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.model.Topic

data class SubjectDetailUiState(
    val subject:     Subject?          = null,
    val topics:      List<Topic>       = emptyList(),
    val assessments: List<Assessment>  = emptyList(),
    val isLoading:   Boolean           = true,
    val error:       String?           = null,
    val estimatingTopicId: Long?       = null,
    val topicEstimationMessage: String? = null
)
