package intellij.kmm.settings.grind_track.feature.settings.ui

import androidx.compose.runtime.Composable

data class PickedSoundFile(
    val displayName: String,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedSoundFile) return false
        return displayName == other.displayName && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * displayName.hashCode() + bytes.contentHashCode()
}

@Composable
expect fun rememberSoundFilePicker(
    onPicked: (PickedSoundFile) -> Unit,
): () -> Unit
