package intellij.kmm.settings.grind_track

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import intellij.kmm.settings.grind_track.app.MainScreen
import intellij.kmm.settings.grind_track.app.ThemeState
import intellij.kmm.settings.grind_track.core.designsystem.GymTrackTheme

@Composable
@Preview
fun App() {
    val themeOverride by ThemeState.isDark.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    GymTrackTheme(darkTheme = themeOverride ?: systemDark) {
        MainScreen()
    }
}
