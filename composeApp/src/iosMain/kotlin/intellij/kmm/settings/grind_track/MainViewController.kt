package intellij.kmm.settings.grind_track

import androidx.compose.ui.window.ComposeUIViewController
import intellij.kmm.settings.grind_track.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = { initKoin() }
) { App() }
