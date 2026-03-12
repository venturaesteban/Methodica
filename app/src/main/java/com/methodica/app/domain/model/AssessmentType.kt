package com.methodica.app.domain.model

/** Catálogo de tipos de evaluación académica. */
enum class AssessmentType(val displayName: String) {
    EXAM("Examen"),
    QUIZ("Quiz"),
    ASSIGNMENT("Tarea"),
    PROJECT("Proyecto");

    companion object {
        fun fromName(name: String): AssessmentType =
            entries.firstOrNull { it.name == name } ?: EXAM
    }
}
