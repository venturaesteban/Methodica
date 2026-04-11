package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.methodica.app.domain.model.Degree
import com.methodica.app.domain.model.DegreeStatus

@Entity(
    tableName = "degrees",
    indices = [Index(value = ["name"], unique = true)]
)
data class DegreeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val status: String = DegreeStatus.ACTIVE.name
)

fun DegreeEntity.toDomain(): Degree = Degree(
    id = id,
    name = name,
    description = description,
    status = DegreeStatus.fromName(status)
)

fun Degree.toEntity(): DegreeEntity = DegreeEntity(
    id = id,
    name = name,
    description = description,
    status = status.name
)

