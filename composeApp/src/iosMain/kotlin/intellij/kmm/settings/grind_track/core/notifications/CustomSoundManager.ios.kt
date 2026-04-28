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

private const val KEY_DISPLAY_NAME = "custom_sound_display_name"
private const val KEY_FILENAME = "custom_sound_filename"
private const val KEY_GENERATION = "custom_sound_generation"

actual class CustomSoundManager(
    private val settings: SettingsStore,
) {
    private val _updates = MutableStateFlow(loadCurrent())
    actual val updates: StateFlow<CustomSound?> = _updates.asStateFlow()

    actual fun current(): CustomSound? = _updates.value

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual suspend fun install(displayName: String, bytes: ByteArray) {
        val previousGeneration = settings.getInt(KEY_GENERATION, 0)
        val previousFilename = settings.getString(KEY_FILENAME)
        val newGeneration = previousGeneration + 1
        val newFilename = "custom_sound_v$newGeneration.wav"

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

        settings.putString(KEY_DISPLAY_NAME, displayName)
        settings.putString(KEY_FILENAME, newFilename)
        settings.putInt(KEY_GENERATION, newGeneration)

        _updates.value = CustomSound(displayName, newFilename)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun uninstall() {
        val filename = settings.getString(KEY_FILENAME)
        if (filename != null) {
            soundsDir()?.URLByAppendingPathComponent(filename)?.let { url ->
                NSFileManager.defaultManager.removeItemAtURL(url, error = null)
            }
        }
        settings.putString(KEY_DISPLAY_NAME, null)
        settings.putString(KEY_FILENAME, null)
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
        val displayName = settings.getString(KEY_DISPLAY_NAME)
        val filename = settings.getString(KEY_FILENAME)
        return if (displayName != null && filename != null) {
            CustomSound(displayName, filename)
        } else null
    }
}
