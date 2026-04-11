package com.methodica.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.methodica.app.feature.academicyear.AcademicYearsScreen
import com.methodica.app.feature.degree.DegreeFormScreen
import com.methodica.app.feature.degree.DegreesScreen
import com.methodica.app.feature.home.HomeScreen
import com.methodica.app.feature.materials.MaterialFormScreen
import com.methodica.app.feature.materials.MaterialsScreen
import com.methodica.app.feature.planning.AiAnalysisScreen
import com.methodica.app.feature.planning.PlanningScreen
import com.methodica.app.feature.settings.SettingsScreen
import com.methodica.app.feature.subjects.AssessmentFormScreen
import com.methodica.app.feature.subjects.SubjectDetailScreen
import com.methodica.app.feature.subjects.SubjectFormScreen
import com.methodica.app.feature.subjects.SubjectsScreen
import com.methodica.app.feature.subjects.TopicFormScreen
import com.methodica.app.feature.today.TodayScreen

@Composable
fun MethodicaNavHost(
    navController: NavHostController,
    modifier:      Modifier = Modifier
) {
    NavHost(
        navController    = navController,
        startDestination = MethodicaDestination.Home.route,
        modifier         = modifier
    ) {
        composable(MethodicaDestination.Home.route) {
            HomeScreen(
                onNavigateToToday = {
                    navController.navigate(MethodicaDestination.Today.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToPlanning = {
                    navController.navigate(MethodicaDestination.Planning.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(MethodicaDestination.Today.route)     { TodayScreen() }
        composable(MethodicaDestination.Planning.route)  {
            PlanningScreen(
                onNavigateToAiAnalysis = { assessmentId ->
                    navController.navigate(MethodicaDestination.AiAnalysis.createRoute(assessmentId))
                }
            )
        }
        composable(MethodicaDestination.Materials.route) {
            MaterialsScreen(
                onNavigateToForm = { materialId ->
                    navController.navigate(MethodicaDestination.MaterialForm.createRoute(materialId))
                }
            )
        }
        composable(MethodicaDestination.Settings.route)  { SettingsScreen() }

        // Rutas para el flujo Degrees → AcademicYears → Subjects
        composable(MethodicaDestination.Degrees.route) {
            DegreesScreen(
                onNavigateToDetail = { degreeId -> 
                    navController.navigate(MethodicaDestination.AcademicYears.createRoute(degreeId)) 
                },
                onNavigateToForm = { degreeId ->
                    navController.navigate(MethodicaDestination.DegreeForm.createRoute(degreeId))
                }
            )
        }

        composable(
            route = MethodicaDestination.DegreeForm.route,
            arguments = listOf(
                navArgument(MethodicaDestination.DegreeForm.ARG_DEGREE_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStack ->
            val raw = backStack.arguments!!.getLong(MethodicaDestination.DegreeForm.ARG_DEGREE_ID)
            val degreeId = if (raw == -1L) null else raw
            DegreeFormScreen(
                degreeId = degreeId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = MethodicaDestination.AcademicYears.route,
            arguments = listOf(
                navArgument(MethodicaDestination.AcademicYears.ARG_DEGREE_ID) { type = NavType.LongType }
            )
        ) { backStack ->
            AcademicYearsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubjects = { academicYearId ->
                    navController.navigate(MethodicaDestination.Subjects.createRoute(academicYearId))
                },
                onNavigateToCreateSubject = { academicYearId ->
                    navController.navigate(MethodicaDestination.SubjectForm.createRoute(academicYearId = academicYearId))
                }
            )
        }

        composable(
            route = MethodicaDestination.Subjects.route,
            arguments = listOf(
                navArgument(MethodicaDestination.Subjects.ARG_ACADEMIC_YEAR_ID) { type = NavType.LongType }
            )
        ) { backStack ->
            val academicYearId = backStack.arguments!!.getLong(MethodicaDestination.Subjects.ARG_ACADEMIC_YEAR_ID)
            SubjectsScreen(
                academicYearId = academicYearId,
                onNavigateToDetail = { id -> navController.navigate(MethodicaDestination.SubjectDetail.createRoute(id)) },
                onNavigateToForm = { id ->
                    if (id == null) {
                        navController.navigate(MethodicaDestination.SubjectForm.createRoute(academicYearId = academicYearId))
                    } else {
                        navController.navigate(MethodicaDestination.SubjectForm.createRoute(subjectId = id))
                    }
                }
            )
        }

        composable(
            route     = MethodicaDestination.SubjectDetail.route,
            arguments = listOf(
                navArgument(MethodicaDestination.SubjectDetail.ARG_SUBJECT_ID) { type = NavType.LongType }
            )
        ) { backStack ->
            val subjectId = backStack.arguments!!.getLong(MethodicaDestination.SubjectDetail.ARG_SUBJECT_ID)
            SubjectDetailScreen(
                onNavigateBack        = { navController.popBackStack() },
                onNavigateToEditForm  = { navController.navigate(MethodicaDestination.SubjectForm.createRoute(subjectId)) },
                onNavigateToTopicForm = { topicId -> navController.navigate(MethodicaDestination.TopicForm.createRoute(subjectId, topicId)) },
                onNavigateToAssessmentForm = { assessmentId -> navController.navigate(MethodicaDestination.AssessmentForm.createRoute(subjectId, assessmentId)) },
                onNavigateToAiAnalysis = { assessmentId ->
                    navController.navigate(MethodicaDestination.AiAnalysis.createRoute(assessmentId))
                }
            )
        }

        composable(
            route     = MethodicaDestination.SubjectForm.route,
            arguments = listOf(
                navArgument(MethodicaDestination.SubjectForm.ARG_SUBJECT_ID) {
                    type         = NavType.LongType
                    defaultValue = -1L
                },
                navArgument(MethodicaDestination.SubjectForm.ARG_ACADEMIC_YEAR_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStack ->
            val raw       = backStack.arguments!!.getLong(MethodicaDestination.SubjectForm.ARG_SUBJECT_ID)
            val subjectId = if (raw == -1L) null else raw
            SubjectFormScreen(
                subjectId      = subjectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route     = MethodicaDestination.TopicForm.route,
            arguments = listOf(
                navArgument(MethodicaDestination.TopicForm.ARG_SUBJECT_ID) { type = NavType.LongType },
                navArgument(MethodicaDestination.TopicForm.ARG_TOPIC_ID) {
                    type         = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStack ->
            val raw       = backStack.arguments!!.getLong(MethodicaDestination.TopicForm.ARG_TOPIC_ID)
            val topicId   = if (raw == -1L) null else raw
            TopicFormScreen(
                topicId        = topicId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route     = MethodicaDestination.AssessmentForm.route,
            arguments = listOf(
                navArgument(MethodicaDestination.AssessmentForm.ARG_SUBJECT_ID) { type = NavType.LongType },
                navArgument(MethodicaDestination.AssessmentForm.ARG_ASSESSMENT_ID) {
                    type         = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStack ->
            val raw          = backStack.arguments!!.getLong(MethodicaDestination.AssessmentForm.ARG_ASSESSMENT_ID)
            val assessmentId = if (raw == -1L) null else raw
            AssessmentFormScreen(
                assessmentId   = assessmentId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = MethodicaDestination.AiAnalysis.route,
            arguments = listOf(
                navArgument(MethodicaDestination.AiAnalysis.ARG_ASSESSMENT_ID) { type = NavType.LongType }
            )
        ) {
            AiAnalysisScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = MethodicaDestination.MaterialForm.route,
            arguments = listOf(
                navArgument(MethodicaDestination.MaterialForm.ARG_MATERIAL_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStack ->
            val raw = backStack.arguments!!.getLong(MethodicaDestination.MaterialForm.ARG_MATERIAL_ID)
            val materialId = if (raw == -1L) null else raw
            MaterialFormScreen(
                materialId = materialId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

