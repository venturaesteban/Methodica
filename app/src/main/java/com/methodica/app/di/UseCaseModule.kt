package com.methodica.app.di

import com.methodica.app.domain.planning.StudyPlanGenerator
import com.methodica.app.domain.ai.ComplexityEstimator
import com.methodica.app.domain.ai.DocumentParser
import com.methodica.app.domain.ai.ExamScopeInferenceService
import com.methodica.app.domain.ai.LlmProvider
import com.methodica.app.domain.ai.StudyPlanningAdvisor
import com.methodica.app.domain.ai.SyllabusAnalyzer
import com.methodica.app.domain.repository.AiAnalysisRepository
import com.methodica.app.domain.repository.AiProviderSettingsRepository
import com.methodica.app.domain.repository.AcademicCatalogRepository
import com.methodica.app.domain.repository.AssessmentRepository
import com.methodica.app.domain.repository.AssessmentTopicRepository
import com.methodica.app.domain.repository.MaterialRepository
import com.methodica.app.domain.repository.PlanningSettingsRepository
import com.methodica.app.domain.repository.StudySessionRepository
import com.methodica.app.domain.repository.SubjectRepository
import com.methodica.app.domain.repository.TopicRepository
import com.methodica.app.domain.usecase.material.DeleteMaterialUseCase
import com.methodica.app.domain.usecase.material.GetMaterialUseCase
import com.methodica.app.domain.usecase.material.ObserveAllMaterialsUseCase
import com.methodica.app.domain.usecase.material.UpsertMaterialUseCase
import com.methodica.app.domain.usecase.academic.EnsureAcademicYearUseCase
import com.methodica.app.domain.usecase.academic.EnsureDegreeUseCase
import com.methodica.app.domain.usecase.academic.ObserveAcademicYearsUseCase
import com.methodica.app.domain.usecase.academic.ObserveDegreesUseCase
import com.methodica.app.domain.usecase.ai.ApplyAiComplexityToTopicsUseCase
import com.methodica.app.domain.usecase.ai.AnalyzeAssessmentWithAiUseCase
import com.methodica.app.domain.usecase.ai.EstimateTopicComplexityUseCase
import com.methodica.app.domain.usecase.ai.GetLatestAiAnalysisForAssessmentUseCase
import com.methodica.app.domain.usecase.ai.ObserveAiProviderSettingsUseCase
import com.methodica.app.domain.usecase.ai.SaveAiAnalysisEditsUseCase
import com.methodica.app.domain.usecase.ai.SaveAiProviderSettingsUseCase
import com.methodica.app.domain.usecase.assessment.DeleteAssessmentUseCase
import com.methodica.app.domain.usecase.assessment.GetAssessmentUseCase
import com.methodica.app.domain.usecase.assessment.ObserveAllAssessmentsUseCase
import com.methodica.app.domain.usecase.assessment.ObserveAssessmentsBySubjectUseCase
import com.methodica.app.domain.usecase.assessment.UpsertAssessmentUseCase
import com.methodica.app.domain.usecase.assessmenttopic.ObserveAssessmentTopicIdsUseCase
import com.methodica.app.domain.usecase.assessmenttopic.ObserveAssessmentTopicsUseCase
import com.methodica.app.domain.usecase.assessmenttopic.ReplaceAssessmentTopicsUseCase
import com.methodica.app.domain.usecase.planning.DeleteAssessmentPlanUseCase
import com.methodica.app.domain.usecase.planning.GenerateAssessmentPlanUseCase
import com.methodica.app.domain.usecase.planning.ObservePlanningSettingsUseCase
import com.methodica.app.domain.usecase.planning.SavePlanningSettingsUseCase
import com.methodica.app.domain.usecase.session.CompleteStudySessionUseCase
import com.methodica.app.domain.usecase.session.ObserveStudySessionsForDateUseCase
import com.methodica.app.domain.usecase.session.ObserveStudySessionsForRangeUseCase
import com.methodica.app.domain.usecase.subject.DeleteSubjectUseCase
import com.methodica.app.domain.usecase.subject.GetSubjectUseCase
import com.methodica.app.domain.usecase.subject.ObserveSubjectsUseCase
import com.methodica.app.domain.usecase.subject.UpsertSubjectUseCase
import com.methodica.app.domain.usecase.topic.DeleteTopicUseCase
import com.methodica.app.domain.usecase.topic.GetTopicUseCase
import com.methodica.app.domain.usecase.topic.ObserveTopicsBySubjectUseCase
import com.methodica.app.domain.usecase.topic.UpsertTopicUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideObserveSubjectsUseCase(repository: SubjectRepository) =
        ObserveSubjectsUseCase(repository)

    @Provides
    @Singleton
    fun provideGetSubjectUseCase(repository: SubjectRepository) =
        GetSubjectUseCase(repository)

    @Provides
    @Singleton
    fun provideUpsertSubjectUseCase(repository: SubjectRepository) =
        UpsertSubjectUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteSubjectUseCase(repository: SubjectRepository) =
        DeleteSubjectUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveTopicsBySubjectUseCase(repository: TopicRepository) =
        ObserveTopicsBySubjectUseCase(repository)

    @Provides
    @Singleton
    fun provideGetTopicUseCase(repository: TopicRepository) =
        GetTopicUseCase(repository)

    @Provides
    @Singleton
    fun provideUpsertTopicUseCase(repository: TopicRepository) =
        UpsertTopicUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteTopicUseCase(repository: TopicRepository) =
        DeleteTopicUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveAssessmentsBySubjectUseCase(repository: AssessmentRepository) =
        ObserveAssessmentsBySubjectUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveAllAssessmentsUseCase(repository: AssessmentRepository) =
        ObserveAllAssessmentsUseCase(repository)

    @Provides
    @Singleton
    fun provideGetAssessmentUseCase(repository: AssessmentRepository) =
        GetAssessmentUseCase(repository)

    @Provides
    @Singleton
    fun provideUpsertAssessmentUseCase(repository: AssessmentRepository) =
        UpsertAssessmentUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteAssessmentUseCase(repository: AssessmentRepository) =
        DeleteAssessmentUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveAssessmentTopicsUseCase(repository: AssessmentTopicRepository) =
        ObserveAssessmentTopicsUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveAssessmentTopicIdsUseCase(repository: AssessmentTopicRepository) =
        ObserveAssessmentTopicIdsUseCase(repository)

    @Provides
    @Singleton
    fun provideReplaceAssessmentTopicsUseCase(repository: AssessmentTopicRepository) =
        ReplaceAssessmentTopicsUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveStudySessionsForDateUseCase(repository: StudySessionRepository) =
        ObserveStudySessionsForDateUseCase(repository)

    @Provides
    @Singleton
    fun provideCompleteStudySessionUseCase(repository: StudySessionRepository) =
        CompleteStudySessionUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveStudySessionsForRangeUseCase(repository: StudySessionRepository) =
        ObserveStudySessionsForRangeUseCase(repository)

    @Provides
    @Singleton
    fun provideStudyPlanGenerator() = StudyPlanGenerator()

    @Provides
    @Singleton
    fun provideGenerateAssessmentPlanUseCase(
        assessmentRepository: AssessmentRepository,
        assessmentTopicRepository: AssessmentTopicRepository,
        studySessionRepository: StudySessionRepository,
        planningSettingsRepository: PlanningSettingsRepository,
        studyPlanGenerator: StudyPlanGenerator
    ) = GenerateAssessmentPlanUseCase(
        assessmentRepository = assessmentRepository,
        assessmentTopicRepository = assessmentTopicRepository,
        studySessionRepository = studySessionRepository,
        settingsRepository = planningSettingsRepository,
        generator = studyPlanGenerator
    )

    @Provides
    @Singleton
    fun provideDeleteAssessmentPlanUseCase(repository: StudySessionRepository) =
        DeleteAssessmentPlanUseCase(repository)

    @Provides
    @Singleton
    fun provideObservePlanningSettingsUseCase(repository: PlanningSettingsRepository) =
        ObservePlanningSettingsUseCase(repository)

    @Provides
    @Singleton
    fun provideSavePlanningSettingsUseCase(repository: PlanningSettingsRepository) =
        SavePlanningSettingsUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveAllMaterialsUseCase(repository: MaterialRepository) =
        ObserveAllMaterialsUseCase(repository)

    @Provides
    @Singleton
    fun provideGetMaterialUseCase(repository: MaterialRepository) =
        GetMaterialUseCase(repository)

    @Provides
    @Singleton
    fun provideUpsertMaterialUseCase(repository: MaterialRepository) =
        UpsertMaterialUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteMaterialUseCase(repository: MaterialRepository) =
        DeleteMaterialUseCase(repository)

    @Provides
    @Singleton
    fun provideAnalyzeAssessmentWithAiUseCase(
        assessmentRepository: AssessmentRepository,
        assessmentTopicRepository: AssessmentTopicRepository,
        aiAnalysisRepository: AiAnalysisRepository,
        aiProviderSettingsRepository: AiProviderSettingsRepository,
        llmProvider: LlmProvider,
        documentParser: DocumentParser,
        syllabusAnalyzer: SyllabusAnalyzer,
        examScopeInferenceService: ExamScopeInferenceService,
        complexityEstimator: ComplexityEstimator,
        studyPlanningAdvisor: StudyPlanningAdvisor
    ) = AnalyzeAssessmentWithAiUseCase(
        assessmentRepository = assessmentRepository,
        assessmentTopicRepository = assessmentTopicRepository,
        aiAnalysisRepository = aiAnalysisRepository,
        aiProviderSettingsRepository = aiProviderSettingsRepository,
        llmProvider = llmProvider,
        documentParser = documentParser,
        syllabusAnalyzer = syllabusAnalyzer,
        examScopeInferenceService = examScopeInferenceService,
        complexityEstimator = complexityEstimator,
        studyPlanningAdvisor = studyPlanningAdvisor
    )

    @Provides
    @Singleton
    fun provideGetLatestAiAnalysisForAssessmentUseCase(
        repository: AiAnalysisRepository
    ) = GetLatestAiAnalysisForAssessmentUseCase(repository)

    @Provides
    @Singleton
    fun provideSaveAiAnalysisEditsUseCase(
        repository: AiAnalysisRepository
    ) = SaveAiAnalysisEditsUseCase(repository)

    @Provides
    @Singleton
    fun provideApplyAiComplexityToTopicsUseCase(
        topicRepository: TopicRepository
    ) = ApplyAiComplexityToTopicsUseCase(topicRepository)

    @Provides
    @Singleton
    fun provideEstimateTopicComplexityUseCase(
        topicRepository: TopicRepository,
        materialRepository: MaterialRepository,
        academicCatalogRepository: AcademicCatalogRepository,
        subjectRepository: SubjectRepository,
        planningSettingsRepository: PlanningSettingsRepository,
        aiProviderSettingsRepository: AiProviderSettingsRepository,
        llmProvider: LlmProvider
    ) = EstimateTopicComplexityUseCase(
        topicRepository = topicRepository,
        materialRepository = materialRepository,
        academicCatalogRepository = academicCatalogRepository,
        subjectRepository = subjectRepository,
        planningSettingsRepository = planningSettingsRepository,
        aiProviderSettingsRepository = aiProviderSettingsRepository,
        llmProvider = llmProvider
    )

    @Provides
    @Singleton
    fun provideObserveAiProviderSettingsUseCase(
        repository: AiProviderSettingsRepository
    ) = ObserveAiProviderSettingsUseCase(repository)

    @Provides
    @Singleton
    fun provideSaveAiProviderSettingsUseCase(
        repository: AiProviderSettingsRepository
    ) = SaveAiProviderSettingsUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveDegreesUseCase(repository: AcademicCatalogRepository) =
        ObserveDegreesUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveAcademicYearsUseCase(repository: AcademicCatalogRepository) =
        ObserveAcademicYearsUseCase(repository)

    @Provides
    @Singleton
    fun provideEnsureDegreeUseCase(repository: AcademicCatalogRepository) =
        EnsureDegreeUseCase(repository)

    @Provides
    @Singleton
    fun provideEnsureAcademicYearUseCase(repository: AcademicCatalogRepository) =
        EnsureAcademicYearUseCase(repository)
}
