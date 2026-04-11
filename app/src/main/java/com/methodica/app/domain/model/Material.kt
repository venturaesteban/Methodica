package com.methodica.app.domain.model

data class Material(
    val id: Long = 0,
    val subjectId: Long,
    val topicId: Long? = null,
    val title: String,
    val uri: String,
    val type: MaterialType,
    val createdAt: Long = System.currentTimeMillis()
)
