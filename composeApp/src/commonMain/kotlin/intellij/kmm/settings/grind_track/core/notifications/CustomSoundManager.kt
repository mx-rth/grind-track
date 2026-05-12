package intellij.kmm.settings.grind_track.core.notifications

import kotlinx.coroutines.flow.StateFlow

data class CustomSound(
    val displayName: String,
    val internalFilename: String,
)

/**
 * Distinguishes the two customisable rest-end sounds. The Notification kind plays at
 * notification volume (stage-1 chime); the Alarm kind plays at alarm volume / loops on
 * Android (stage-2 follow-up at +15 s).
 */
enum class CustomSoundKind { Notification, Alarm }

expect class CustomSoundManager {
    val updates: StateFlow<CustomSound?>
    val gainDb: StateFlow<Float>
    fun current(): CustomSound?
    suspend fun install(displayName: String, bytes: ByteArray)
    suspend fun setGainDb(db: Float)
    fun uninstall()
}
