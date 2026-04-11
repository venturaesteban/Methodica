package com.methodica.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.methodica.app.core.designsystem.theme.MethodicaTheme
import com.methodica.app.core.navigation.MethodicaDestination
import com.methodica.app.core.navigation.MethodicaNavHost
import com.methodica.app.core.navigation.TopLevelDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MethodicaApp() {
    val navController       = rememberNavController()
    val navBackStackEntry   by navController.currentBackStackEntryAsState()
    val currentRoute        = navBackStackEntry?.destination?.route

    MethodicaTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(currentRoute.toScreenTitle()) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    navigationIcon = {
                        if (!TopLevelDestination.isTopLevel(currentRoute)) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Atrás"
                                )
                            }
                        }
                    },
                    actions = {
                        // El icono de ajustes solo se oculta si ya estás en Settings
                        if (currentRoute != MethodicaDestination.Settings.route) {
                            IconButton(
                                onClick = {
                                    navController.navigate(MethodicaDestination.Settings.route) {
                                        launchSingleTop = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector        = Icons.Filled.Settings,
                                    contentDescription = "Ajustes"
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                // La barra inferior solo se muestra en destinos de nivel superior
                if (TopLevelDestination.isTopLevel(currentRoute)) {
                    MethodicaBottomBar(
                        currentRoute = currentRoute,
                        onNavigate   = { destination ->
                            navController.navigate(destination.route) {
                                // Evita duplicar destinos en el backstack al re-seleccionar
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            MethodicaNavHost(
                navController = navController,
                modifier      = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun MethodicaBottomBar(
    currentRoute: String?,
    onNavigate:   (TopLevelDestination) -> Unit
) {
    val glassColor = androidx.compose.material3.MaterialTheme.colorScheme
        .surfaceContainerLowest
        .copy(alpha = 0.8f)
    NavigationBar(containerColor = glassColor, tonalElevation = 0.dp) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick  = { onNavigate(destination) },
                icon     = {
                    Icon(
                        imageVector        = destination.icon,
                        contentDescription = destination.label
                    )
                },
                label    = { Text(destination.label) }
            )
        }
    }
}

private fun String?.toScreenTitle(): String = when (this) {
    MethodicaDestination.Home.route           -> "Inicio"
    MethodicaDestination.Today.route          -> "Hoy"
    MethodicaDestination.Degrees.route        -> "Estudios"
    MethodicaDestination.Planning.route       -> "Planificación"
    MethodicaDestination.Materials.route      -> "Materiales"
    MethodicaDestination.Settings.route       -> "Ajustes"
    MethodicaDestination.AiAnalysis.route     -> "Análisis IA"
    MethodicaDestination.DegreeForm.route     -> "Titulación"
    MethodicaDestination.AcademicYears.route  -> "Cursos"
    MethodicaDestination.Subjects.route       -> "Materias"
    MethodicaDestination.SubjectDetail.route  -> "Detalle de materia"
    MethodicaDestination.SubjectForm.route    -> "Materia"
    MethodicaDestination.TopicForm.route      -> "Tema"
    MethodicaDestination.AssessmentForm.route -> "Evaluación"
    MethodicaDestination.MaterialForm.route   -> "Material"
    else                                      -> "Methodica"
}
