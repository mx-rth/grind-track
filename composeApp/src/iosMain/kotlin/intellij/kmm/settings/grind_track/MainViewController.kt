package intellij.kmm.settings.grind_track

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.ComposeUIViewController
import intellij.kmm.settings.grind_track.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = { initKoin() }
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = 1f,
        )
    ) {
        App()
    }
}
