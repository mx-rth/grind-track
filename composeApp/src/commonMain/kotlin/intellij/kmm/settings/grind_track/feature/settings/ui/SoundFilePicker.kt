package intellij.kmm.settings.grind_track.feature.settings.ui

import androidx.compose.runtime.Composable

data class PickedSoundFile(
    val displayName: String,
    val bytes: ByteArray,
)

@Composable
expect fun rememberSoundFilePicker(
    onPicked: (PickedSoundFile) -> Unit,
): () -> Unit
