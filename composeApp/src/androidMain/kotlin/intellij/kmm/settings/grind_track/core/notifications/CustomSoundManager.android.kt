package intellij.kmm.settings.grind_track.core.notifications

import android.content.Context
import android.net.Uri
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

private val CustomSoundKind.settingsPrefix: String
    get() = when (this) {
        CustomSoundKind.Notification -> "custom_sound_notification_"
        CustomSoundKind.Alarm -> "custom_sound_alarm_"
    }

private val CustomSoundKind.filenamePrefix: String
    get() = when (this) {
        CustomSoundKind.Notification -> "custom_notification_v"
        CustomSoundKind.Alarm -> "custom_alarm_v"
    }

private val CustomSoundKind.defaultChannelId: String
    get() = when (this) {
        CustomSoundKind.Notification -> REST_TIMER_COMPLETE_CHANNEL_ID
        CustomSoundKind.Alarm -> REST_TIMER_CHANNEL_ID
    }

actual class CustomSoundManager(
    private val context: Context,
    private val settings: SettingsStore,
    private val kind: CustomSoundKind,
) {
    private val keyDisplayName = "${kind.settingsPrefix}display_name"
    private val keyFilename = "${kind.settingsPrefix}filename"
    private val keyGeneration = "${kind.settingsPrefix}generation"
    private val keyGainDb = "${kind.settingsPrefix}gain_db"

    private val _updates = MutableStateFlow(loadCurrent())
    actual val updates: StateFlow<CustomSound?> = _updates.asStateFlow()

    private val _gainDb = MutableStateFlow(settings.getFloat(keyGainDb, 0f))
    actual val gainDb: StateFlow<Float> = _gainDb.asStateFlow()

    actual fun current(): CustomSound? = _updates.value

    actual suspend fun install(displayName: String, bytes: ByteArray) {
        val previousGeneration = settings.getInt(keyGeneration, 0)
        val previousFilename = settings.getString(keyFilename)
        val newGeneration = previousGeneration + 1
        val newFilename = "${kind.filenamePrefix}$newGeneration.wav"
        val newOrigFilename = origFilenameOf(newFilename)

        withContext(Dispatchers.IO) {
            val dir = soundsDir().also { it.mkdirs() }
            File(dir, newOrigFilename).writeBytes(bytes)
            val processed = WavAmplifier.amplify(bytes, 0f)
            File(dir, newFilename).writeBytes(processed)
            previousFilename?.let {
                File(dir, it).delete()
                File(dir, origFilenameOf(it)).delete()
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            File(soundsDir(), newFilename),
        )
        ensureCustomChannel(context, kind, uri, newGeneration)
        if (previousGeneration > 0) deleteCustomChannel(context, kind, previousGeneration)

        settings.putString(keyDisplayName, displayName)
        settings.putString(keyFilename, newFilename)
        settings.putInt(keyGeneration, newGeneration)
        settings.putFloat(keyGainDb, 0f)

        _updates.value = CustomSound(displayName, newFilename)
        _gainDb.value = 0f
    }

    actual suspend fun setGainDb(db: Float) {
        val clamped = db.coerceIn(0f, 12f)
        val current = _updates.value ?: return
        val previousGeneration = settings.getInt(keyGeneration, 0)
        val previousFilename = current.internalFilename
        val previousOrigFilename = origFilenameOf(previousFilename)
        val newGeneration = previousGeneration + 1
        val newFilename = "${kind.filenamePrefix}$newGeneration.wav"
        val newOrigFilename = origFilenameOf(newFilename)

        withContext(Dispatchers.IO) {
            val dir = soundsDir().also { it.mkdirs() }
            val origFile = File(dir, previousOrigFilename)
            if (!origFile.exists()) return@withContext
            val origBytes = origFile.readBytes()
            File(dir, newOrigFilename).writeBytes(origBytes)
            val processed = WavAmplifier.amplify(origBytes, clamped)
            File(dir, newFilename).writeBytes(processed)
            File(dir, previousFilename).delete()
            origFile.delete()
        }

        val uri = FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            File(soundsDir(), newFilename),
        )
        ensureCustomChannel(context, kind, uri, newGeneration)
        if (previousGeneration > 0) deleteCustomChannel(context, kind, previousGeneration)

        settings.putString(keyFilename, newFilename)
        settings.putInt(keyGeneration, newGeneration)
        settings.putFloat(keyGainDb, clamped)

        _updates.value = CustomSound(current.displayName, newFilename)
        _gainDb.value = clamped
    }

    actual fun uninstall() {
        val filename = settings.getString(keyFilename)
        val generation = settings.getInt(keyGeneration, 0)
        if (filename != null) {
            File(soundsDir(), filename).delete()
            File(soundsDir(), origFilenameOf(filename)).delete()
        }
        if (generation > 0) deleteCustomChannel(context, kind, generation)
        settings.putString(keyDisplayName, null)
        settings.putString(keyFilename, null)
        settings.putFloat(keyGainDb, 0f)
        // Generation counter is intentionally NOT reset, so a future install gets a fresh
        // channel ID (Android caches deleted channel IDs for a window).

        _updates.value = null
        _gainDb.value = 0f
    }

    /**
     * Returns the channel id to post to for this manager's [kind]: the per-generation
     * custom channel if a custom sound is installed, otherwise the kind-appropriate
     * default channel ([REST_TIMER_COMPLETE_CHANNEL_ID] for Notification,
     * [REST_TIMER_CHANNEL_ID] for Alarm).
     */
    fun currentChannelId(): String {
        val current = _updates.value ?: return kind.defaultChannelId
        val generation = settings.getInt(keyGeneration, 0)
        return if (generation > 0 && current.internalFilename.isNotBlank()) {
            customChannelId(kind, generation)
        } else {
            kind.defaultChannelId
        }
    }

    /**
     * FileProvider URI for the currently-installed custom sound, or null if none is
     * installed or the underlying file is missing.
     */
    fun currentSoundUri(): Uri? {
        val current = _updates.value ?: return null
        val file = File(soundsDir(), current.internalFilename)
        if (!file.exists()) return null
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
    }

    /**
     * Re-create the notification channel for the currently-installed custom sound, if
     * any. Idempotent — `ensureCustomChannel` skips creation when the channel already
     * exists. Call this before scheduling so we recover from edge cases like an app
     * reinstall that preserved SharedPreferences but wiped channels, or a system cleanup
     * that removed the channel. If the underlying sound file is gone, clears the stale
     * settings so callers fall back to the default sound.
     */
    fun ensureChannel() {
        val current = _updates.value ?: return
        val generation = settings.getInt(keyGeneration, 0)
        if (generation <= 0) return
        val file = File(soundsDir(), current.internalFilename)
        if (!file.exists()) {
            uninstall()
            return
        }
        val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        ensureCustomChannel(context, kind, uri, generation)
    }

    private fun soundsDir(): File = File(context.filesDir, SOUNDS_DIR)

    private fun origFilenameOf(processedFilename: String): String =
        processedFilename.removeSuffix(".wav") + "_orig.wav"

    private fun loadCurrent(): CustomSound? {
        val displayName = settings.getString(keyDisplayName)
        val filename = settings.getString(keyFilename)
        return if (displayName != null && filename != null) {
            CustomSound(displayName, filename)
        } else null
    }
}
