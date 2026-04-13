package com.methodica.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.methodica.app.data.local.dao.AssessmentDao
import com.methodica.app.data.local.dao.AssessmentTopicDao
import com.methodica.app.data.local.dao.AcademicYearDao
import com.methodica.app.data.local.dao.AiAnalysisDao
import com.methodica.app.data.local.dao.AiChunkEmbeddingDao
import com.methodica.app.data.local.dao.AiDocumentChunkDao
import com.methodica.app.data.local.dao.AiDocumentDao
import com.methodica.app.data.local.dao.AiIndexingRunDao
import com.methodica.app.data.local.dao.DegreeDao
import com.methodica.app.data.local.dao.ExamScopeAnalysisDao
import com.methodica.app.data.local.dao.MaterialDao
import com.methodica.app.data.local.dao.StudySessionDao
import com.methodica.app.data.local.dao.SubjectDao
import com.methodica.app.data.local.dao.TopicComplexityAnalysisDao
import com.methodica.app.data.local.dao.TopicDao
import com.methodica.app.data.local.dao.LocalAiModelStateDao
import com.methodica.app.data.local.entity.AssessmentEntity
import com.methodica.app.data.local.entity.AssessmentTopicCrossRef
import com.methodica.app.data.local.entity.AcademicYearEntity
import com.methodica.app.data.local.entity.AiAnalysisEntity
import com.methodica.app.data.local.entity.AiChunkEmbeddingEntity
import com.methodica.app.data.local.entity.AiDocumentChunkEntity
import com.methodica.app.data.local.entity.AiDocumentEntity
import com.methodica.app.data.local.entity.AiIndexingRunEntity
import com.methodica.app.data.local.entity.DegreeEntity
import com.methodica.app.data.local.entity.ExamScopeAnalysisEntity
import com.methodica.app.data.local.entity.MaterialEntity
import com.methodica.app.data.local.entity.StudySessionEntity
import com.methodica.app.data.local.entity.SubjectEntity
import com.methodica.app.data.local.entity.TopicComplexityAnalysisEntity
import com.methodica.app.data.local.entity.TopicEntity
import com.methodica.app.data.local.entity.LocalAiModelStateEntity

/**
 * Base de datos Room. Es la ÚNICA fuente de verdad para datos académicos.
 *
 * Version 8: se añade estado de titulacion (en curso, completada, cerrada).
 */
@Database(
    entities = [
        SubjectEntity::class,
        DegreeEntity::class,
        AcademicYearEntity::class,
        TopicEntity::class,
        AssessmentEntity::class,
        StudySessionEntity::class,
        AssessmentTopicCrossRef::class,
        MaterialEntity::class,
        AiDocumentEntity::class,
        AiAnalysisEntity::class,
        ExamScopeAnalysisEntity::class,
        TopicComplexityAnalysisEntity::class,
        AiDocumentChunkEntity::class,
        AiChunkEmbeddingEntity::class,
        AiIndexingRunEntity::class,
        LocalAiModelStateEntity::class
    ],
    version      = 11,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun degreeDao(): DegreeDao
    abstract fun academicYearDao(): AcademicYearDao
    abstract fun topicDao(): TopicDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun assessmentTopicDao(): AssessmentTopicDao
    abstract fun materialDao(): MaterialDao
    abstract fun aiDocumentDao(): AiDocumentDao
    abstract fun aiDocumentChunkDao(): AiDocumentChunkDao
    abstract fun aiChunkEmbeddingDao(): AiChunkEmbeddingDao
    abstract fun aiIndexingRunDao(): AiIndexingRunDao
    abstract fun aiAnalysisDao(): AiAnalysisDao
    abstract fun examScopeAnalysisDao(): ExamScopeAnalysisDao
    abstract fun topicComplexityAnalysisDao(): TopicComplexityAnalysisDao
    abstract fun localAiModelStateDao(): LocalAiModelStateDao
}
