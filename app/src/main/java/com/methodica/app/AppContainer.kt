package com.methodica.app

import android.content.Context
import androidx.room.Room
import com.methodica.app.data.local.AppDatabase
import com.methodica.app.data.preferences.PlanningSettingsRepositoryImpl
import com.methodica.app.data.repository.AssessmentRepositoryImpl
import com.methodica.app.data.repository.AssessmentTopicRepositoryImpl
import com.methodica.app.data.repository.StudySessionRepositoryImpl
import com.methodica.app.data.repository.SubjectRepositoryImpl
import com.methodica.app.data.repository.TopicRepositoryImpl
import com.methodica.app.domain.planning.StudyPlanGenerator
import com.methodica.app.domain.repository.AssessmentRepository
import com.methodica.app.domain.repository.AssessmentTopicRepository
import com.methodica.app.domain.repository.PlanningSettingsRepository
import com.methodica.app.domain.repository.StudySessionRepository
import com.methodica.app.domain.repository.SubjectRepository
import com.methodica.app.domain.repository.TopicRepository
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
import com.methodica.app.domain.usecase.session.ObserveStudySessionsForAssessmentUseCase
import com.methodica.app.domain.usecase.session.ObserveStudySessionsForDateUseCase
import com.methodica.app.domain.usecase.subject.DeleteSubjectUseCase
import com.methodica.app.domain.usecase.subject.GetSubjectUseCase
import com.methodica.app.domain.usecase.subject.ObserveSubjectsUseCase
import com.methodica.app.domain.usecase.subject.UpsertSubjectUseCase
import com.methodica.app.domain.usecase.topic.DeleteTopicUseCase
import com.methodica.app.domain.usecase.topic.GetTopicUseCase
import com.methodica.app.domain.usecase.topic.ObserveTopicsBySubjectUseCase
import com.methodica.app.domain.usecase.topic.UpsertTopicUseCase

/**
 * Composition root manual de la app.
 * En lugar de un framework DI (Hilt/Koin), usamos un contenedor simple
 * para mantener el MVP sin dependencias adicionales.
 */
interface AppContainer {
    val subjectRepository:          SubjectRepository
    val topicRepository:            TopicRepository
    val assessmentRepository:       AssessmentRepository
    val studySessionRepository:     StudySessionRepository
    val assessmentTopicRepository:  AssessmentTopicRepository
    val planningSettingsRepository: PlanningSettingsRepository

    val observeSubjectsUseCase:          ObserveSubjectsUseCase
    val getSubjectUseCase:               GetSubjectUseCase
    val upsertSubjectUseCase:            UpsertSubjectUseCase
    val deleteSubjectUseCase:            DeleteSubjectUseCase

    val observeTopicsBySubjectUseCase:   ObserveTopicsBySubjectUseCase
    val getTopicUseCase:                 GetTopicUseCase
    val upsertTopicUseCase:              UpsertTopicUseCase
    val deleteTopicUseCase:              DeleteTopicUseCase

    val observeAssessmentsBySubjectUseCase: ObserveAssessmentsBySubjectUseCase
    val observeAllAssessmentsUseCase:       ObserveAllAssessmentsUseCase
    val getAssessmentUseCase:               GetAssessmentUseCase
    val upsertAssessmentUseCase:            UpsertAssessmentUseCase
    val deleteAssessmentUseCase:            DeleteAssessmentUseCase

    val observeAssessmentTopicsUseCase:    ObserveAssessmentTopicsUseCase
    val observeAssessmentTopicIdsUseCase:  ObserveAssessmentTopicIdsUseCase
    val replaceAssessmentTopicsUseCase:    ReplaceAssessmentTopicsUseCase

    val observeStudySessionsForDateUseCase:       ObserveStudySessionsForDateUseCase
    val observeStudySessionsForAssessmentUseCase: ObserveStudySessionsForAssessmentUseCase
    val completeStudySessionUseCase:              CompleteStudySessionUseCase

    val generateAssessmentPlanUseCase: GenerateAssessmentPlanUseCase
    val deleteAssessmentPlanUseCase:   DeleteAssessmentPlanUseCase
    val observePlanningSettingsUseCase: ObservePlanningSettingsUseCase
    val savePlanningSettingsUseCase:    SavePlanningSettingsUseCase
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "methodica.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // --- Repositories ---

    override val subjectRepository: SubjectRepository by lazy {
        SubjectRepositoryImpl(database.subjectDao())
    }

    override val topicRepository: TopicRepository by lazy {
        TopicRepositoryImpl(database.topicDao())
    }

    override val assessmentRepository: AssessmentRepository by lazy {
        AssessmentRepositoryImpl(database.assessmentDao())
    }

    override val studySessionRepository: StudySessionRepository by lazy {
        StudySessionRepositoryImpl(database.studySessionDao())
    }

    override val assessmentTopicRepository: AssessmentTopicRepository by lazy {
        AssessmentTopicRepositoryImpl(database.assessmentTopicDao())
    }

    override val planningSettingsRepository: PlanningSettingsRepository by lazy {
        PlanningSettingsRepositoryImpl(context.applicationContext)
    }

    // --- Subject use cases ---

    override val observeSubjectsUseCase by lazy { ObserveSubjectsUseCase(subjectRepository) }
    override val getSubjectUseCase      by lazy { GetSubjectUseCase(subjectRepository) }
    override val upsertSubjectUseCase   by lazy { UpsertSubjectUseCase(subjectRepository) }
    override val deleteSubjectUseCase   by lazy { DeleteSubjectUseCase(subjectRepository) }

    // --- Topic use cases ---

    override val observeTopicsBySubjectUseCase by lazy { ObserveTopicsBySubjectUseCase(topicRepository) }
    override val getTopicUseCase               by lazy { GetTopicUseCase(topicRepository) }
    override val upsertTopicUseCase            by lazy { UpsertTopicUseCase(topicRepository) }
    override val deleteTopicUseCase            by lazy { DeleteTopicUseCase(topicRepository) }

    // --- Assessment use cases ---

    override val observeAssessmentsBySubjectUseCase by lazy { ObserveAssessmentsBySubjectUseCase(assessmentRepository) }
    override val observeAllAssessmentsUseCase       by lazy { ObserveAllAssessmentsUseCase(assessmentRepository) }
    override val getAssessmentUseCase               by lazy { GetAssessmentUseCase(assessmentRepository) }
    override val upsertAssessmentUseCase            by lazy { UpsertAssessmentUseCase(assessmentRepository) }
    override val deleteAssessmentUseCase            by lazy { DeleteAssessmentUseCase(assessmentRepository) }

    // --- Assessment-Topic use cases ---

    override val observeAssessmentTopicsUseCase   by lazy { ObserveAssessmentTopicsUseCase(assessmentTopicRepository) }
    override val observeAssessmentTopicIdsUseCase by lazy { ObserveAssessmentTopicIdsUseCase(assessmentTopicRepository) }
    override val replaceAssessmentTopicsUseCase   by lazy { ReplaceAssessmentTopicsUseCase(assessmentTopicRepository) }

    // --- Session use cases ---

    override val observeStudySessionsForDateUseCase       by lazy { ObserveStudySessionsForDateUseCase(studySessionRepository) }
    override val observeStudySessionsForAssessmentUseCase by lazy { ObserveStudySessionsForAssessmentUseCase(studySessionRepository) }
    override val completeStudySessionUseCase              by lazy { CompleteStudySessionUseCase(studySessionRepository) }

    // --- Planning use cases ---

    private val studyPlanGenerator by lazy { StudyPlanGenerator() }

    override val generateAssessmentPlanUseCase by lazy {
        GenerateAssessmentPlanUseCase(
            assessmentRepository, assessmentTopicRepository,
            studySessionRepository, planningSettingsRepository, studyPlanGenerator
        )
    }
    override val deleteAssessmentPlanUseCase    by lazy { DeleteAssessmentPlanUseCase(studySessionRepository) }
    override val observePlanningSettingsUseCase  by lazy { ObservePlanningSettingsUseCase(planningSettingsRepository) }
    override val savePlanningSettingsUseCase     by lazy { SavePlanningSettingsUseCase(planningSettingsRepository) }
}

