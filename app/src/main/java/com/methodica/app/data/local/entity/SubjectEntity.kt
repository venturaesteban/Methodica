package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.methodica.app.domain.model.Subject

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id:          Long    = 0,
    val name:        String,
    val colorHex:    String,
    val description: String? = null
)

// --- Mappers entre capa data y capa domain ---

fun SubjectEntity.toDomain(): Subject =
    Subject(id = id, name = name, colorHex = colorHex, description = description)

fun Subject.toEntity(): SubjectEntity =
    SubjectEntity(id = id, name = name, colorHex = colorHex, description = description)
