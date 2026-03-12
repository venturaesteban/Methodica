package com.methodica.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.methodica.app.MethodicaApplication
import com.methodica.app.feature.home.HomeScreen
import com.methodica.app.feature.materials.MaterialsScreen
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
    val context   = LocalContext.current
    val container = remember { (context.applicationContext as MethodicaApplication).container }

    NavHost(
        navController    = navController,
        startDestination = MethodicaDestination.Home.route,
        modifier         = modifier
    ) {
        composable(MethodicaDestination.Home.route)      { HomeScreen() }
        composable(MethodicaDestination.Today.route)     { TodayScreen(container = container) }
        composable(MethodicaDestination.Planning.route)  { PlanningScreen(container = container) }
        composable(MethodicaDestination.Materials.route) { MaterialsScreen() }
        composable(MethodicaDestination.Settings.route)  { SettingsScreen(container = container) }

        composable(MethodicaDestination.Subjects.route) {
            SubjectsScreen(
                container          = container,
                onNavigateToDetail = { id -> navController.navigate(MethodicaDestination.SubjectDetail.createRoute(id)) },
                onNavigateToForm   = { id -> navController.navigate(MethodicaDestination.SubjectForm.createRoute(id)) }
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
                subjectId             = subjectId,
                container             = container,
                onNavigateBack        = { navController.popBackStack() },
                onNavigateToEditForm  = { navController.navigate(MethodicaDestination.SubjectForm.createRoute(subjectId)) },
                onNavigateToTopicForm = { topicId -> navController.navigate(MethodicaDestination.TopicForm.createRoute(subjectId, topicId)) },
                onNavigateToAssessmentForm = { assessmentId -> navController.navigate(MethodicaDestination.AssessmentForm.createRoute(subjectId, assessmentId)) }
            )
        }

        composable(
            route     = MethodicaDestination.SubjectForm.route,
            arguments = listOf(
                navArgument(MethodicaDestination.SubjectForm.ARG_SUBJECT_ID) {
                    type         = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStack ->
            val raw       = backStack.arguments!!.getLong(MethodicaDestination.SubjectForm.ARG_SUBJECT_ID)
            val subjectId = if (raw == -1L) null else raw
            SubjectFormScreen(
                subjectId      = subjectId,
                container      = container,
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
            val subjectId = backStack.arguments!!.getLong(MethodicaDestination.TopicForm.ARG_SUBJECT_ID)
            val raw       = backStack.arguments!!.getLong(MethodicaDestination.TopicForm.ARG_TOPIC_ID)
            val topicId   = if (raw == -1L) null else raw
            TopicFormScreen(
                subjectId      = subjectId,
                topicId        = topicId,
                container      = container,
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
            val subjectId    = backStack.arguments!!.getLong(MethodicaDestination.AssessmentForm.ARG_SUBJECT_ID)
            val raw          = backStack.arguments!!.getLong(MethodicaDestination.AssessmentForm.ARG_ASSESSMENT_ID)
            val assessmentId = if (raw == -1L) null else raw
            AssessmentFormScreen(
                subjectId      = subjectId,
                assessmentId   = assessmentId,
                container      = container,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

