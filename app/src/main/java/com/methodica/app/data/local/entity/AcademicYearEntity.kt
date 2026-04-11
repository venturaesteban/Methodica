package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.methodica.app.domain.model.AcademicYear

@Entity(
    tableName = "academic_years",
    foreignKeys = [
        ForeignKey(
            entity = DegreeEntity::class,
            parentColumns = ["id"],
            childColumns = ["degreeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("degreeId"),
        Index(value = ["degreeId", "yearNumber"], unique = true)
    ]
)
data class AcademicYearEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val degreeId: Long,
    val yearNumber: Int,
    val name: String
)

fun AcademicYearEntity.toDomain(): AcademicYear = AcademicYear(
    id = id,
    degreeId = degreeId,
    yearNumber = yearNumber,
    name = name
)

fun AcademicYear.toEntity(): AcademicYearEntity = AcademicYearEntity(
    id = id,
    degreeId = degreeId,
    yearNumber = yearNumber,
    name = name
)

