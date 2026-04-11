package com.methodica.app.domain.model

enum class DegreeStatus(val displayName: String) {
    ACTIVE("En curso"),
    COMPLETED("Completada"),
    CLOSED("Cerrada");

    companion object {
        fun fromName(name: String?): DegreeStatus =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ACTIVE
    }
}
