package com.methodica.app.di

import android.content.Context
import com.methodica.app.data.ai.ExternalLlmProvider
import com.methodica.app.data.ai.HeuristicComplexityEstimator
import com.methodica.app.data.ai.HeuristicDocumentParser
import com.methodica.app.data.ai.HeuristicExamScopeInferenceService
import com.methodica.app.data.ai.HeuristicStudyPlanningAdvisor
import com.methodica.app.data.ai.HeuristicSyllabusAnalyzer
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
import com.methodica.app.data.preferences.PlanningSettingsRepositoryImpl
import com.methodica.app.data.preferences.AiProviderSettingsRepositoryImpl
import com.methodica.app.data.local.AppDatabase
import com.methodica.app.data.repository.AiAnalysisRepositoryImpl
import com.methodica.app.data.repository.AcademicCatalogRepositoryImpl
import com.methodica.app.data.repository.AssessmentRepositoryImpl
import com.methodica.app.data.repository.AssessmentTopicRepositoryImpl
import com.methodica.app.data.repository.MaterialRepositoryImpl
import com.methodica.app.data.repository.StudySessionRepositoryImpl
import com.methodica.app.data.repository.SubjectRepositoryImpl
import com.methodica.app.data.repository.TopicRepositoryImpl
import com.methodica.app.domain.repository.AssessmentRepository
import com.methodica.app.domain.repository.AssessmentTopicRepository
import com.methodica.app.domain.ai.ComplexityEstimator
import com.methodica.app.domain.ai.DocumentParser
import com.methodica.app.domain.ai.ExamScopeInferenceService
import com.methodica.app.domain.ai.LlmProvider
import com.methodica.app.domain.ai.StudyPlanningAdvisor
import com.methodica.app.domain.ai.SyllabusAnalyzer
import com.methodica.app.domain.repository.AiAnalysisRepository
import com.methodica.app.domain.repository.AiProviderSettingsRepository
import com.methodica.app.domain.repository.AcademicCatalogRepository
import com.methodica.app.domain.repository.MaterialRepository
import com.methodica.app.domain.repository.PlanningSettingsRepository
import com.methodica.app.domain.repository.StudySessionRepository
import com.methodica.app.domain.repository.SubjectRepository
import com.methodica.app.domain.repository.TopicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSubjectRepository(dao: SubjectDao): SubjectRepository =
        SubjectRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideTopicRepository(dao: TopicDao): TopicRepository =
        TopicRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideAssessmentRepository(dao: AssessmentDao): AssessmentRepository =
        AssessmentRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideStudySessionRepository(dao: StudySessionDao): StudySessionRepository =
        StudySessionRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideAssessmentTopicRepository(dao: AssessmentTopicDao): AssessmentTopicRepository =
        AssessmentTopicRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideMaterialRepository(
        dao: MaterialDao,
        @ApplicationContext context: Context
    ): MaterialRepository = MaterialRepositoryImpl(
        dao = dao,
        context = context
    )

    @Provides
    @Singleton
    fun provideAiAnalysisRepository(
        database: AppDatabase,
        aiDocumentDao: AiDocumentDao,
        aiAnalysisDao: AiAnalysisDao,
        examScopeAnalysisDao: ExamScopeAnalysisDao,
        topicComplexityAnalysisDao: TopicComplexityAnalysisDao
    ): AiAnalysisRepository = AiAnalysisRepositoryImpl(
        database = database,
        aiDocumentDao = aiDocumentDao,
        aiAnalysisDao = aiAnalysisDao,
        examScopeAnalysisDao = examScopeAnalysisDao,
        topicComplexityAnalysisDao = topicComplexityAnalysisDao
    )

    @Provides
    @Singleton
    fun provideLlmProvider(): LlmProvider = ExternalLlmProvider()

    @Provides
    @Singleton
    fun provideAiProviderSettingsRepository(
        @ApplicationContext context: Context
    ): AiProviderSettingsRepository = AiProviderSettingsRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideDocumentParser(): DocumentParser = HeuristicDocumentParser()

    @Provides
    @Singleton
    fun provideSyllabusAnalyzer(): SyllabusAnalyzer = HeuristicSyllabusAnalyzer()

    @Provides
    @Singleton
    fun provideExamScopeInferenceService(): ExamScopeInferenceService = HeuristicExamScopeInferenceService()

    @Provides
    @Singleton
    fun provideComplexityEstimator(): ComplexityEstimator = HeuristicComplexityEstimator()

    @Provides
    @Singleton
    fun provideStudyPlanningAdvisor(): StudyPlanningAdvisor = HeuristicStudyPlanningAdvisor()

    @Provides
    @Singleton
    fun providePlanningSettingsRepository(
        @ApplicationContext context: Context
    ): PlanningSettingsRepository = PlanningSettingsRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideAcademicCatalogRepository(
        degreeDao: DegreeDao,
        academicYearDao: AcademicYearDao
    ): AcademicCatalogRepository = AcademicCatalogRepositoryImpl(
        degreeDao = degreeDao,
        academicYearDao = academicYearDao
    )
}
