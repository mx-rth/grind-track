package intellij.kmm.settings.grind_track.feature.progress.ui

import androidx.compose.runtime.Composable
import intellij.kmm.settings.grind_track.core.designsystem.EmptyState

@Composable
fun ProgressScreen() {
    EmptyState(
        title = "No progress yet",
        subtitle = "Complete a workout to start seeing PRs and trends.",
    )
}
