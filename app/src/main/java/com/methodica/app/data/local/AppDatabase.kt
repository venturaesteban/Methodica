package com.methodica.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.methodica.app.data.local.dao.AssessmentDao
import com.methodica.app.data.local.dao.AssessmentTopicDao
import com.methodica.app.data.local.dao.StudySessionDao
import com.methodica.app.data.local.dao.SubjectDao
import com.methodica.app.data.local.dao.TopicDao
import com.methodica.app.data.local.entity.AssessmentEntity
import com.methodica.app.data.local.entity.AssessmentTopicCrossRef
import com.methodica.app.data.local.entity.StudySessionEntity
import com.methodica.app.data.local.entity.SubjectEntity
import com.methodica.app.data.local.entity.TopicEntity

/**
 * Base de datos Room. Es la ÚNICA fuente de verdad para datos académicos.
 *
 * Versión 3: se añaden study_sessions y assessment_topic_cross_ref para
 * el motor de planificación. Se usa fallbackToDestructiveMigration
 * mientras no haya datos reales de producción; se debe migrar explícitamente
 * antes de la primera release pública.
 */
@Database(
    entities = [
        SubjectEntity::class,
        TopicEntity::class,
        AssessmentEntity::class,
        StudySessionEntity::class,
        AssessmentTopicCrossRef::class
    ],
    version      = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun assessmentTopicDao(): AssessmentTopicDao
}
