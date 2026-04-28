package intellij.kmm.settings.grind_track.core.notifications

import kotlinx.coroutines.flow.StateFlow

data class CustomSound(
    val displayName: String,
    val internalFilename: String,
)

expect class CustomSoundManager {
    val updates: StateFlow<CustomSound?>
    fun current(): CustomSound?
    suspend fun install(displayName: String, bytes: ByteArray)
    fun uninstall()
}
