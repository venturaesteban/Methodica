package com.methodica.app.core.navigation

/**
 * Contrato central de rutas de la app.
 * Todas las navegaciones deben pasar por estos objetos; nunca por strings literales dispersos.
 */
sealed class MethodicaDestination(val route: String) {
    data object Home      : MethodicaDestination("home")
    data object Today     : MethodicaDestination("today")
    data object Subjects  : MethodicaDestination("subjects")
    data object Planning  : MethodicaDestination("planning")
    data object Materials : MethodicaDestination("materials")
    data object Settings  : MethodicaDestination("settings")

    // Rutas internas de la feature Subjects (no aparecen en la barra inferior)
    object SubjectDetail : MethodicaDestination("subjectDetail/{subjectId}") {
        const val ARG_SUBJECT_ID = "subjectId"
        fun createRoute(subjectId: Long) = "subjectDetail/$subjectId"
    }

    object SubjectForm : MethodicaDestination("subjectForm?subjectId={subjectId}") {
        const val ARG_SUBJECT_ID = "subjectId"
        /** subjectId == null → modo creación; != null → modo edición */
        fun createRoute(subjectId: Long? = null) =
            if (subjectId != null) "subjectForm?subjectId=$subjectId" else "subjectForm"
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
}

