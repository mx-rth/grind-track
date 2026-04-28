package intellij.kmm.settings.grind_track.core.notifications

import android.content.Context
import androidx.core.content.FileProvider
import intellij.kmm.settings.grind_track.core.preferences.SettingsStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

private const val FILE_PROVIDER_AUTHORITY = "intellij.kmm.settings.grind_track.fileprovider"
private const val SOUNDS_DIR = "sounds"

private const val KEY_DISPLAY_NAME = "custom_sound_display_name"
private const val KEY_FILENAME = "custom_sound_filename"
private const val KEY_GENERATION = "custom_sound_generation"

actual class CustomSoundManager(
    private val context: Context,
    private val settings: SettingsStore,
) {
    private val _updates = MutableStateFlow(loadCurrent())
    actual val updates: StateFlow<CustomSound?> = _updates.asStateFlow()

    actual fun current(): CustomSound? = _updates.value

    actual suspend fun install(displayName: String, bytes: ByteArray) {
        val previousGeneration = settings.getInt(KEY_GENERATION, 0)
        val previousFilename = settings.getString(KEY_FILENAME)
        val newGeneration = previousGeneration + 1
        val newFilename = "custom_sound_v$newGeneration.wav"

        withContext(Dispatchers.IO) {
            val dir = soundsDir().also { it.mkdirs() }
            File(dir, newFilename).writeBytes(bytes)
            previousFilename?.let { File(dir, it).delete() }
        }

        val uri = FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            File(soundsDir(), newFilename),
        )
        ensureCustomRestTimerChannel(context, uri, newGeneration)
        if (previousGeneration > 0) deleteCustomRestTimerChannel(context, previousGeneration)

        settings.putString(KEY_DISPLAY_NAME, displayName)
        settings.putString(KEY_FILENAME, newFilename)
        settings.putInt(KEY_GENERATION, newGeneration)

        _updates.value = CustomSound(displayName, newFilename)
    }

    actual fun uninstall() {
        val filename = settings.getString(KEY_FILENAME)
        val generation = settings.getInt(KEY_GENERATION, 0)
        if (filename != null) File(soundsDir(), filename).delete()
        if (generation > 0) deleteCustomRestTimerChannel(context, generation)
        settings.putString(KEY_DISPLAY_NAME, null)
        settings.putString(KEY_FILENAME, null)
        // Note: generation counter is intentionally NOT reset, so a future install
        // gets a fresh channel ID (Android caches deleted channel IDs for a window).

        _updates.value = null
    }

    /**
     * Returns the current notification channel id to post to: the custom channel if
     * a custom sound is installed, otherwise the default rest-timer channel id.
     */
    fun currentChannelId(): String {
        val current = _updates.value ?: return REST_TIMER_CHANNEL_ID
        val generation = settings.getInt(KEY_GENERATION, 0)
        return if (generation > 0 && current.internalFilename.isNotBlank()) {
            customRestTimerChannelId(generation)
        } else {
            REST_TIMER_CHANNEL_ID
        }
    }

    private fun soundsDir(): File = File(context.filesDir, SOUNDS_DIR)

    private fun loadCurrent(): CustomSound? {
        val displayName = settings.getString(KEY_DISPLAY_NAME)
        val filename = settings.getString(KEY_FILENAME)
        return if (displayName != null && filename != null) {
            CustomSound(displayName, filename)
        } else null
    }
}
