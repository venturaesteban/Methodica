package com.methodica.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.methodica.app.domain.model.Material
import com.methodica.app.domain.model.MaterialType

@Entity(
    tableName = "materials",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("subjectId"),
        Index("topicId"),
        Index("type")
    ]
)
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val topicId: Long?,
    val title: String,
    val uri: String,
    val type: String,
    val createdAt: Long
)

fun MaterialEntity.toDomain(): Material = Material(
    id = id,
    subjectId = subjectId,
    topicId = topicId,
    title = title,
    uri = uri,
    type = runCatching { MaterialType.valueOf(type) }.getOrDefault(MaterialType.WEB_LINK),
    createdAt = createdAt
)

fun Material.toEntity(): MaterialEntity = MaterialEntity(
    id = id,
    subjectId = subjectId,
    topicId = topicId,
    title = title,
    uri = uri,
    type = type.name,
    createdAt = createdAt
)
