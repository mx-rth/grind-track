package intellij.kmm.settings.grind_track.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import intellij.kmm.settings.grind_track.feature.progress.ui.ProgressScreen
import intellij.kmm.settings.grind_track.feature.routines.ui.RoutineEditorScreen
import intellij.kmm.settings.grind_track.feature.routines.ui.RoutinesScreen
import intellij.kmm.settings.grind_track.feature.settings.ui.SettingsScreen
import intellij.kmm.settings.grind_track.feature.workout.ui.WorkoutScreen

private const val ROUTINE_EDITOR_ROUTE = "routine"
private const val ROUTINE_ID_ARG = "routineId"
private const val SETTINGS_ROUTE = "settings"

internal fun routineEditorRoute(routineId: Long): String = "$ROUTINE_EDITOR_ROUTE/$routineId"

@Composable
fun GymTrackNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Routines.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.Routines.route) {
            RoutinesScreen(
                onOpenRoutine = { routineId ->
                    navController.navigate(routineEditorRoute(routineId))
                },
                onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
            )
        }
        composable(TopLevelDestination.Workout.route) { WorkoutScreen() }
        composable(TopLevelDestination.Progress.route) { ProgressScreen() }
        composable(
            route = "$ROUTINE_EDITOR_ROUTE/{$ROUTINE_ID_ARG}",
            arguments = listOf(navArgument(ROUTINE_ID_ARG) { type = NavType.LongType }),
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.read { getLong(ROUTINE_ID_ARG) }
                ?: return@composable
            RoutineEditorScreen(
                routineId = routineId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(SETTINGS_ROUTE) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
