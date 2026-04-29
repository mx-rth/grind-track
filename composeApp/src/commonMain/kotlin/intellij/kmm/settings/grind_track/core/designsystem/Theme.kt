package intellij.kmm.settings.grind_track.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object BrandColors {
    val SunYellow = Color(0xFFFFDA60)
    val SunYellowSoft = Color(0xFFFFEEA8)
    val Coral = Color(0xFFFF6F66)
    val CoralSoft = Color(0xFFFFD4D0)
    val Electric = Color(0xFF3D5AFE)
    val ElectricSoft = Color(0xFFC8D1FF)
    val Cyan = Color(0xFF5BCED9)
    val CyanSoft = Color(0xFFC8EEF2)
    val Magenta = Color(0xFFA93D70)
    val MagentaSoft = Color(0xFFF7C8DB)
    val InkNavy = Color(0xFF1F2933)
    val InkNavyDeep = Color(0xFF111820)
    val Slate = Color(0xFF5C6B7A)
    val SlateSoft = Color(0xFFD9DEE3)
    val Cream = Color(0xFFF5F4ED)
    val CreamWarm = Color(0xFFFFFBEC)
    val MintFresh = Color(0xFF8AE6B6)
}

private val LightColors = lightColorScheme(
    primary = BrandColors.InkNavy,
    onPrimary = Color.White,
    primaryContainer = BrandColors.SunYellow,
    onPrimaryContainer = BrandColors.InkNavy,
    secondary = BrandColors.Coral,
    onSecondary = Color.White,
    secondaryContainer = BrandColors.CoralSoft,
    onSecondaryContainer = Color(0xFF541512),
    tertiary = BrandColors.Electric,
    onTertiary = Color.White,
    tertiaryContainer = BrandColors.ElectricSoft,
    onTertiaryContainer = Color(0xFF0A1372),
    background = BrandColors.Cream,
    onBackground = BrandColors.InkNavy,
    surface = Color(0xFFFAF9F2),
    onSurface = BrandColors.InkNavy,
    surfaceVariant = Color(0xFFEAE8DD),
    onSurfaceVariant = BrandColors.Slate,
    outline = BrandColors.SlateSoft,
    outlineVariant = Color(0xFFE2E0D6),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = BrandColors.SunYellow,
    onPrimary = BrandColors.InkNavyDeep,
    primaryContainer = Color(0xFF8A6A00),
    onPrimaryContainer = BrandColors.SunYellowSoft,
    secondary = BrandColors.Coral,
    onSecondary = BrandColors.InkNavyDeep,
    secondaryContainer = Color(0xFF7A2A24),
    onSecondaryContainer = BrandColors.CoralSoft,
    tertiary = Color(0xFF8C9DFF),
    onTertiary = BrandColors.InkNavyDeep,
    tertiaryContainer = Color(0xFF1E2A85),
    onTertiaryContainer = BrandColors.ElectricSoft,
    background = BrandColors.InkNavyDeep,
    onBackground = Color(0xFFE7E6E1),
    surface = BrandColors.InkNavy,
    onSurface = Color(0xFFE7E6E1),
    surfaceVariant = Color(0xFF2A3340),
    onSurfaceVariant = Color(0xFFB0BAC5),
    outline = Color(0xFF3D4A5A),
    outlineVariant = Color(0xFF2A3340),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

private val GymTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val GymTrackTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 56.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, letterSpacing = (-0.25).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.6.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.6.sp),
)

@Composable
fun GymTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        shapes = GymTrackShapes,
        typography = GymTrackTypography,
        content = content,
    )
}
