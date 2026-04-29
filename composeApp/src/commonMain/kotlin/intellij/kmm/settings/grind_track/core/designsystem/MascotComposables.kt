package intellij.kmm.settings.grind_track.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

/**
 * Provides the currently selected mascot variant to the composition. Wrap the
 * app root in `CompositionLocalProvider(LocalMascotVariant provides ...)` so
 * any screen can read it without threading it through every parameter.
 */
val LocalMascotVariant = compositionLocalOf { MascotVariant.Female }

private const val MascotAspectRatio = 307f / 512f

/**
 * Renders the user's mascot in the given pose. Falls back to the [MascotVariant.Female]
 * variant when no preference has been provided to the composition.
 */
@Composable
fun Mascot(
    pose: MascotPose,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val variant = LocalMascotVariant.current
    Image(
        painter = painterResource(mascotResource(variant, pose)),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
            .height(size)
            .aspectRatio(MascotAspectRatio, matchHeightConstraintsFirst = true),
    )
}
