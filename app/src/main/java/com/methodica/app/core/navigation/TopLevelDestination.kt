package com.methodica.app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Los 5 destinos de nivel superior que aparecen en la BottomNavigationBar.
 * Settings queda fuera de la barra y se accede desde el TopAppBar.
 */
enum class TopLevelDestination(
    val route:     String,
    val icon:      ImageVector,
    val label:     String
) {
    HOME(
        route = MethodicaDestination.Home.route,
        icon  = Icons.Filled.Home,
        label = "Inicio"
    ),
    TODAY(
        route = MethodicaDestination.Today.route,
        icon  = Icons.Filled.Today,
        label = "Hoy"
    ),
    DEGREES(
        route = MethodicaDestination.Degrees.route,
        icon  = Icons.AutoMirrored.Filled.MenuBook,
        label = "Estudios"
    ),
    PLANNING(
        route = MethodicaDestination.Planning.route,
        icon  = Icons.Filled.CalendarMonth,
        label = "Plan"
    ),
    MATERIALS(
        route = MethodicaDestination.Materials.route,
        icon  = Icons.Filled.Folder,
        label = "Recursos"
    );

    companion object {
        /** Comprueba si una ruta dada corresponde a un destino de nivel superior */
        fun isTopLevel(route: String?): Boolean = entries.any { it.route == route }
    }
}
