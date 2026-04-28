package intellij.kmm.settings.grind_track.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF386A1F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F397),
    onPrimaryContainer = Color(0xFF042100),
    secondary = Color(0xFF54634D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E8CD),
    onSecondaryContainer = Color(0xFF121F0E),
    tertiary = Color(0xFF386568),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFCFDF6),
    onBackground = Color(0xFF1A1C18),
    surface = Color(0xFFFCFDF6),
    onSurface = Color(0xFF1A1C18),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD67D),
    onPrimary = Color(0xFF0D3900),
    primaryContainer = Color(0xFF1F5108),
    onPrimaryContainer = Color(0xFFB7F397),
    secondary = Color(0xFFBBCBB2),
    onSecondary = Color(0xFF263422),
    secondaryContainer = Color(0xFF3C4B37),
    onSecondaryContainer = Color(0xFFD7E8CD),
    tertiary = Color(0xFFA0CFD2),
    onTertiary = Color(0xFF003738),
    background = Color(0xFF1A1C18),
    onBackground = Color(0xFFE3E3DC),
    surface = Color(0xFF1A1C18),
    onSurface = Color(0xFFE3E3DC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun GymTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
