package intellij.kmm.settings.grind_track

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import intellij.kmm.settings.grind_track.app.MainScreen
import intellij.kmm.settings.grind_track.core.designsystem.GymTrackTheme

@Composable
@Preview
fun App() {
    GymTrackTheme {
        MainScreen()
    }
}
