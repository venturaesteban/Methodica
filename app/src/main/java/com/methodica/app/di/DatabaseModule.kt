package com.methodica.app.di

import android.content.Context
import androidx.room.Room
import com.methodica.app.data.local.AppDatabase
import com.methodica.app.data.local.AppDatabaseMigrations
import com.methodica.app.data.local.dao.AssessmentDao
import com.methodica.app.data.local.dao.AssessmentTopicDao
import com.methodica.app.data.local.dao.AcademicYearDao
import com.methodica.app.data.local.dao.AiAnalysisDao
import com.methodica.app.data.local.dao.AiDocumentDao
import com.methodica.app.data.local.dao.DegreeDao
import com.methodica.app.data.local.dao.ExamScopeAnalysisDao
import com.methodica.app.data.local.dao.MaterialDao
import com.methodica.app.data.local.dao.StudySessionDao
import com.methodica.app.data.local.dao.SubjectDao
import com.methodica.app.data.local.dao.TopicComplexityAnalysisDao
import com.methodica.app.data.local.dao.TopicDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "methodica.db"
    )
        .addMigrations(*AppDatabaseMigrations.ALL)
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideSubjectDao(database: AppDatabase): SubjectDao = database.subjectDao()

    @Provides
    fun provideDegreeDao(database: AppDatabase): DegreeDao = database.degreeDao()

    @Provides
    fun provideAcademicYearDao(database: AppDatabase): AcademicYearDao = database.academicYearDao()

    @Provides
    fun provideTopicDao(database: AppDatabase): TopicDao = database.topicDao()

    @Provides
    fun provideAssessmentDao(database: AppDatabase): AssessmentDao = database.assessmentDao()

    @Provides
    fun provideStudySessionDao(database: AppDatabase): StudySessionDao = database.studySessionDao()

    @Provides
    fun provideAssessmentTopicDao(database: AppDatabase): AssessmentTopicDao = database.assessmentTopicDao()

    @Provides
    fun provideMaterialDao(database: AppDatabase): MaterialDao = database.materialDao()

    @Provides
    fun provideAiDocumentDao(database: AppDatabase): AiDocumentDao = database.aiDocumentDao()

    @Provides
    fun provideAiAnalysisDao(database: AppDatabase): AiAnalysisDao = database.aiAnalysisDao()

    @Provides
    fun provideExamScopeAnalysisDao(database: AppDatabase): ExamScopeAnalysisDao = database.examScopeAnalysisDao()

    @Provides
    fun provideTopicComplexityAnalysisDao(database: AppDatabase): TopicComplexityAnalysisDao =
        database.topicComplexityAnalysisDao()
}
