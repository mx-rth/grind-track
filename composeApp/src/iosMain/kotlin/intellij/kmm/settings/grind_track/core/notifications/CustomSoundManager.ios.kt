package intellij.kmm.settings.grind_track.core.notifications

import intellij.kmm.settings.grind_track.core.preferences.SettingsStore
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL

private const val SOUNDS_DIR_NAME = "Sounds"

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

actual class CustomSoundManager(
    private val settings: SettingsStore,
    private val kind: CustomSoundKind,
) {
    private val keyDisplayName = "${kind.settingsPrefix}display_name"
    private val keyFilename = "${kind.settingsPrefix}filename"
    private val keyGeneration = "${kind.settingsPrefix}generation"

    private val _updates = MutableStateFlow(loadCurrent())
    actual val updates: StateFlow<CustomSound?> = _updates.asStateFlow()

    actual fun current(): CustomSound? = _updates.value

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual suspend fun install(displayName: String, bytes: ByteArray) {
        val previousGeneration = settings.getInt(keyGeneration, 0)
        val previousFilename = settings.getString(keyFilename)
        val newGeneration = previousGeneration + 1
        val newFilename = "${kind.filenamePrefix}$newGeneration.wav"

        withContext(Dispatchers.Default) {
            val soundsDir = ensureSoundsDir()
            val target = soundsDir.URLByAppendingPathComponent(newFilename)
                ?: error("Could not compute Sounds URL for $newFilename")
            val data = bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
            data.writeToURL(target, atomically = true)

            if (previousFilename != null) {
                soundsDir.URLByAppendingPathComponent(previousFilename)?.let { previous ->
                    NSFileManager.defaultManager.removeItemAtURL(previous, error = null)
                }
            }
        }

        settings.putString(keyDisplayName, displayName)
        settings.putString(keyFilename, newFilename)
        settings.putInt(keyGeneration, newGeneration)

        _updates.value = CustomSound(displayName, newFilename)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun uninstall() {
        val filename = settings.getString(keyFilename)
        if (filename != null) {
            soundsDir()?.URLByAppendingPathComponent(filename)?.let { url ->
                NSFileManager.defaultManager.removeItemAtURL(url, error = null)
            }
        }
        settings.putString(keyDisplayName, null)
        settings.putString(keyFilename, null)
        _updates.value = null
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun soundsDir(): NSURL? {
        val library = NSFileManager.defaultManager.URLForDirectory(
            directory = NSLibraryDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        ) ?: return null
        return library.URLByAppendingPathComponent(SOUNDS_DIR_NAME)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun ensureSoundsDir(): NSURL {
        val dir = soundsDir() ?: error("Could not resolve Library directory")
        NSFileManager.defaultManager.createDirectoryAtURL(
            url = dir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return dir
    }

    private fun loadCurrent(): CustomSound? {
        val displayName = settings.getString(keyDisplayName)
        val filename = settings.getString(keyFilename)
        return if (displayName != null && filename != null) {
            CustomSound(displayName, filename)
        } else null
    }
}
