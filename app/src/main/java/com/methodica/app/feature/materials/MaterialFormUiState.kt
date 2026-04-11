package com.methodica.app.feature.materials

import com.methodica.app.domain.model.MaterialType
import com.methodica.app.domain.model.Subject
import com.methodica.app.domain.model.Topic

data class MaterialFormUiState(
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null,
    val title: String = "",
    val titleError: String? = null,
    val uri: String = "",
    val uriError: String? = null,
    val pickedFileMimeType: String? = null,
    val type: MaterialType = MaterialType.WEB_LINK,
    val subjects: List<Subject> = emptyList(),
    val selectedDegreeId: Long? = null,
    val selectedCourseYear: Int? = null,
    val selectedSubjectId: Long? = null,
    val topics: List<Topic> = emptyList(),
    val selectedTopicId: Long? = null
) {
    val availableDegrees: List<Pair<Long, String>>
        get() = subjects
            .map { it.degreeId to it.degreeName }
            .distinctBy { it.first }
            .sortedBy { it.second }

    val availableCourseYears: List<Int>
        get() = subjects
            .asSequence()
            .filter { selectedDegreeId == null || it.degreeId == selectedDegreeId }
            .map { it.courseYear }
            .distinct()
            .sorted()
            .toList()

    val availableSubjects: List<Subject>
        get() = subjects
            .asSequence()
            .filter { selectedDegreeId == null || it.degreeId == selectedDegreeId }
            .filter { selectedCourseYear == null || it.courseYear == selectedCourseYear }
            .toList()
}
