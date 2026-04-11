package com.methodica.app.data.repository

import androidx.room.withTransaction
import com.methodica.app.data.local.AppDatabase
import com.methodica.app.data.local.dao.AiAnalysisDao
import com.methodica.app.data.local.dao.AiDocumentDao
import com.methodica.app.data.local.dao.ExamScopeAnalysisDao
import com.methodica.app.data.local.dao.TopicComplexityAnalysisDao
import com.methodica.app.data.local.entity.toDomain
import com.methodica.app.data.local.entity.toEntity
import com.methodica.app.domain.model.AiAnalysis
import com.methodica.app.domain.model.AiDocument
import com.methodica.app.domain.model.ExamScopeAnalysis
import com.methodica.app.domain.model.TopicComplexityAnalysis
import com.methodica.app.domain.repository.AiAnalysisRepository
import com.methodica.app.domain.repository.StoredAiAnalysis

class AiAnalysisRepositoryImpl(
    private val database: AppDatabase,
    private val aiDocumentDao: AiDocumentDao,
    private val aiAnalysisDao: AiAnalysisDao,
    private val examScopeAnalysisDao: ExamScopeAnalysisDao,
    private val topicComplexityAnalysisDao: TopicComplexityAnalysisDao
) : AiAnalysisRepository {

    override suspend fun storeAnalysis(
        document: AiDocument,
        analysis: AiAnalysis,
        scope: ExamScopeAnalysis,
        topicComplexities: List<TopicComplexityAnalysis>
    ): StoredAiAnalysis {
        return database.withTransaction {
            val documentId = aiDocumentDao.insert(document.toEntity())
            val analysisId = aiAnalysisDao.insert(analysis.copy(documentId = documentId).toEntity())
            examScopeAnalysisDao.insert(scope.copy(analysisId = analysisId).toEntity())
            topicComplexityAnalysisDao.insertAll(
                topicComplexities.map { it.copy(analysisId = analysisId).toEntity() }
            )

            val storedDocument = aiDocumentDao.getById(documentId)!!.toDomain()
            val storedAnalysis = aiAnalysisDao.getById(analysisId)!!.toDomain()
            val storedScope = examScopeAnalysisDao.getByAnalysisId(analysisId)!!.toDomain()
            val storedTopics = topicComplexityAnalysisDao.getByAnalysisId(analysisId).map { it.toDomain() }

            StoredAiAnalysis(
                document = storedDocument,
                analysis = storedAnalysis,
                scope = storedScope,
                topicComplexities = storedTopics
            )
        }
    }

    override suspend fun getLatestAnalysisForAssessment(assessmentId: Long): StoredAiAnalysis? {
        val analysisEntity = aiAnalysisDao.getLatestByAssessmentId(assessmentId) ?: return null
        val documentEntity = aiDocumentDao.getById(analysisEntity.documentId) ?: return null
        val scopeEntity = examScopeAnalysisDao.getByAnalysisId(analysisEntity.id) ?: return null
        val topicEntities = topicComplexityAnalysisDao.getByAnalysisId(analysisEntity.id)

        return StoredAiAnalysis(
            document = documentEntity.toDomain(),
            analysis = analysisEntity.toDomain(),
            scope = scopeEntity.toDomain(),
            topicComplexities = topicEntities.map { it.toDomain() }
        )
    }

    override suspend fun saveUserEdits(
        analysisId: Long,
        estimatedScope: String,
        justification: String,
        topicComplexities: List<TopicComplexityAnalysis>
    ) {
        database.withTransaction {
            val scope = examScopeAnalysisDao.getByAnalysisId(analysisId)
            if (scope != null) {
                examScopeAnalysisDao.update(
                    scope.copy(
                        estimatedScope = estimatedScope,
                        justification = justification,
                        requiresUserConfirmation = false
                    )
                )
            }

            topicComplexityAnalysisDao.deleteByAnalysisId(analysisId)
            topicComplexityAnalysisDao.insertAll(
                topicComplexities.map {
                    it.copy(analysisId = analysisId).toEntity()
                }
            )
        }
    }
}
