package intellij.kmm.settings.grind_track.feature.workout.ui

import androidx.compose.runtime.Composable
import intellij.kmm.settings.grind_track.core.designsystem.EmptyState

@Composable
fun WorkoutScreen() {
    EmptyState(
        title = "Start a workout",
        subtitle = "Pick a routine to begin. Active sessions will live here.",
    )
}
