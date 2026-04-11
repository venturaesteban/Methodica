package com.methodica.app.feature.materials

import com.methodica.app.domain.model.Material
import com.methodica.app.domain.model.MaterialType
import com.methodica.app.domain.model.Subject

data class MaterialsUiState(
	val isLoading: Boolean = true,
	val error: String? = null,
	val subjects: List<Subject> = emptyList(),
	val materials: List<Material> = emptyList(),
	val selectedDegreeId: Long? = null,
	val selectedCourseYear: Int? = null,
	val selectedSubjectId: Long? = null,
	val selectedType: MaterialType? = null
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

	val filteredMaterials: List<Material>
		get() = materials
			.asSequence()
			.filter { material ->
				val subject = subjects.firstOrNull { it.id == material.subjectId }
				(selectedDegreeId == null || subject?.degreeId == selectedDegreeId) &&
				(selectedCourseYear == null || subject?.courseYear == selectedCourseYear)
			}
			.filter { selectedSubjectId == null || it.subjectId == selectedSubjectId }
			.filter { selectedType == null || it.type == selectedType }
			.toList()
}
