package com.methodica.app.feature.subjects

import com.methodica.app.domain.model.AssessmentType
import com.methodica.app.domain.model.Topic

data class AssessmentFormUiState(
    val title:            String         = "",
    val type:             AssessmentType = AssessmentType.EXAM,
    val date:             Long           = System.currentTimeMillis(),
    val weight:           String         = "",
    val notes:            String         = "",
    val isLoading:        Boolean        = false,
    val isSaved:          Boolean        = false,
    val titleError:       String?        = null,
    val weightError:      String?        = null,
    val error:            String?        = null,
    val availableTopics:  List<Topic>    = emptyList(),
    val selectedTopicIds: Set<Long>      = emptySet()
)
