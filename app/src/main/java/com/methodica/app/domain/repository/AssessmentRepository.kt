package com.methodica.app.domain.repository

import com.methodica.app.domain.model.Assessment
import kotlinx.coroutines.flow.Flow

interface AssessmentRepository {
    fun observeAllAssessments(): Flow<List<Assessment>>
    fun observeAssessments(subjectId: Long): Flow<List<Assessment>>
    suspend fun getAssessment(id: Long): Assessment?
    suspend fun saveAssessment(assessment: Assessment): Long
    suspend fun deleteAssessment(assessment: Assessment)
}
