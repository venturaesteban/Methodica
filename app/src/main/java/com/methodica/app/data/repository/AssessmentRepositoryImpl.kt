package com.methodica.app.data.repository

import com.methodica.app.data.local.dao.AssessmentDao
import com.methodica.app.data.local.entity.toDomain
import com.methodica.app.data.local.entity.toEntity
import com.methodica.app.domain.model.Assessment
import com.methodica.app.domain.repository.AssessmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AssessmentRepositoryImpl(
    private val dao: AssessmentDao
) : AssessmentRepository {

    override fun observeAllAssessments(): Flow<List<Assessment>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeAssessments(subjectId: Long): Flow<List<Assessment>> =
        dao.observeBySubjectId(subjectId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAssessment(id: Long): Assessment? =
        dao.getById(id)?.toDomain()

    override suspend fun saveAssessment(assessment: Assessment): Long =
        dao.upsert(assessment.toEntity())

    override suspend fun deleteAssessment(assessment: Assessment) =
        dao.delete(assessment.toEntity())
}
