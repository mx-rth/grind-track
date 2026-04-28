package intellij.kmm.settings.grind_track.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TopLevelDestination(val route: String, val label: String, val icon: ImageVector) {
    data object Routines : TopLevelDestination("routines", "Routines", Icons.Filled.List)
    data object Workout : TopLevelDestination("workout", "Workout", Icons.Filled.PlayArrow)
    data object Progress : TopLevelDestination("progress", "Progress", Icons.Filled.Star)

    companion object {
        val all: List<TopLevelDestination> = listOf(Routines, Workout, Progress)
    }
}
