package com.methodica.app.core.navigation

/**
 * Contrato central de rutas de la app.
 * Todas las navegaciones deben pasar por estos objetos; nunca por strings literales dispersos.
 */
sealed class MethodicaDestination(val route: String) {
    data object Home      : MethodicaDestination("home")
    data object Today     : MethodicaDestination("today")
    data object Degrees   : MethodicaDestination("degrees")
    data object Planning  : MethodicaDestination("planning")
    data object AiAnalysis : MethodicaDestination("aiAnalysis/{assessmentId}") {
        const val ARG_ASSESSMENT_ID = "assessmentId"
        fun createRoute(assessmentId: Long) = "aiAnalysis/$assessmentId"
    }
    data object Materials : MethodicaDestination("materials")
    data object Settings  : MethodicaDestination("settings")

    // Rutas internas de la feature degree (no aparecen en la barra inferior)
    object AcademicYears : MethodicaDestination("academicYears/{degreeId}") {
        const val ARG_DEGREE_ID = "degreeId"
        fun createRoute(degreeId: Long) = "academicYears/$degreeId"
    }

    object DegreeForm : MethodicaDestination("degreeForm?degreeId={degreeId}") {
        const val ARG_DEGREE_ID = "degreeId"
        fun createRoute(degreeId: Long? = null) =
            if (degreeId != null) "degreeForm?degreeId=$degreeId" else "degreeForm"
    }

    // Rutas internas de la feature Subjects (no aparecen en la barra inferior)
    object Subjects : MethodicaDestination("subjects/{academicYearId}") {
        const val ARG_ACADEMIC_YEAR_ID = "academicYearId"
        fun createRoute(academicYearId: Long) = "subjects/$academicYearId"
    }

    object SubjectDetail : MethodicaDestination("subjectDetail/{subjectId}") {
        const val ARG_SUBJECT_ID = "subjectId"
        fun createRoute(subjectId: Long) = "subjectDetail/$subjectId"
    }

    object SubjectForm : MethodicaDestination("subjectForm?subjectId={subjectId}&academicYearId={academicYearId}") {
        const val ARG_SUBJECT_ID = "subjectId"
        const val ARG_ACADEMIC_YEAR_ID = "academicYearId"
        /** subjectId == null → modo creación; != null → modo edición */
        fun createRoute(subjectId: Long? = null, academicYearId: Long? = null): String {
            val params = buildList {
                if (subjectId != null) add("subjectId=$subjectId")
                if (academicYearId != null) add("academicYearId=$academicYearId")
            }
            return if (params.isEmpty()) "subjectForm" else "subjectForm?${params.joinToString("&")}" 
        }
    }

    object TopicForm : MethodicaDestination("topicForm/{subjectId}?topicId={topicId}") {
        const val ARG_SUBJECT_ID = "subjectId"
        const val ARG_TOPIC_ID   = "topicId"
        fun createRoute(subjectId: Long, topicId: Long? = null) =
            if (topicId != null) "topicForm/$subjectId?topicId=$topicId" else "topicForm/$subjectId"
    }

    object AssessmentForm : MethodicaDestination("assessmentForm/{subjectId}?assessmentId={assessmentId}") {
        const val ARG_SUBJECT_ID    = "subjectId"
        const val ARG_ASSESSMENT_ID = "assessmentId"
        fun createRoute(subjectId: Long, assessmentId: Long? = null) =
            if (assessmentId != null) "assessmentForm/$subjectId?assessmentId=$assessmentId"
            else "assessmentForm/$subjectId"
    }

    object MaterialForm : MethodicaDestination("materialForm?materialId={materialId}") {
        const val ARG_MATERIAL_ID = "materialId"
        fun createRoute(materialId: Long? = null) =
            if (materialId != null) "materialForm?materialId=$materialId" else "materialForm"
    }
}

