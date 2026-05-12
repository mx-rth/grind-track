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
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL
import platform.posix.memcpy

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

private fun origFilenameOf(processedFilename: String): String =
    processedFilename.removeSuffix(".wav") + "_orig.wav"

actual class CustomSoundManager(
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

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual suspend fun install(displayName: String, bytes: ByteArray) {
        val previousGeneration = settings.getInt(keyGeneration, 0)
        val previousFilename = settings.getString(keyFilename)
        val newGeneration = previousGeneration + 1
        val newFilename = "${kind.filenamePrefix}$newGeneration.wav"
        val newOrigFilename = origFilenameOf(newFilename)

        withContext(Dispatchers.Default) {
            val soundsDir = ensureSoundsDir()
            writeBytes(soundsDir, newOrigFilename, bytes)
            val processed = WavAmplifier.amplify(bytes, 0f)
            writeBytes(soundsDir, newFilename, processed)

            if (previousFilename != null) {
                deleteFile(soundsDir, previousFilename)
                deleteFile(soundsDir, origFilenameOf(previousFilename))
            }
        }

        settings.putString(keyDisplayName, displayName)
        settings.putString(keyFilename, newFilename)
        settings.putInt(keyGeneration, newGeneration)
        settings.putFloat(keyGainDb, 0f)

        _updates.value = CustomSound(displayName, newFilename)
        _gainDb.value = 0f
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual suspend fun setGainDb(db: Float) {
        val clamped = db.coerceIn(0f, 12f)
        val current = _updates.value ?: return
        val previousGeneration = settings.getInt(keyGeneration, 0)
        val previousFilename = current.internalFilename
        val previousOrigFilename = origFilenameOf(previousFilename)
        val newGeneration = previousGeneration + 1
        val newFilename = "${kind.filenamePrefix}$newGeneration.wav"
        val newOrigFilename = origFilenameOf(newFilename)

        val applied = withContext(Dispatchers.Default) {
            val soundsDir = ensureSoundsDir()
            val origBytes = readBytes(soundsDir, previousOrigFilename) ?: return@withContext false
            writeBytes(soundsDir, newOrigFilename, origBytes)
            val processed = WavAmplifier.amplify(origBytes, clamped)
            writeBytes(soundsDir, newFilename, processed)
            deleteFile(soundsDir, previousFilename)
            deleteFile(soundsDir, previousOrigFilename)
            true
        }
        if (!applied) return

        settings.putString(keyFilename, newFilename)
        settings.putInt(keyGeneration, newGeneration)
        settings.putFloat(keyGainDb, clamped)

        _updates.value = CustomSound(current.displayName, newFilename)
        _gainDb.value = clamped
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun uninstall() {
        val filename = settings.getString(keyFilename)
        if (filename != null) {
            soundsDir()?.let { dir ->
                deleteFile(dir, filename)
                deleteFile(dir, origFilenameOf(filename))
            }
        }
        settings.putString(keyDisplayName, null)
        settings.putString(keyFilename, null)
        settings.putFloat(keyGainDb, 0f)
        _updates.value = null
        _gainDb.value = 0f
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

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun writeBytes(dir: NSURL, filename: String, bytes: ByteArray) {
        val target = dir.URLByAppendingPathComponent(filename)
            ?: error("Could not compute URL for $filename")
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        data.writeToURL(target, atomically = true)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun readBytes(dir: NSURL, filename: String): ByteArray? {
        val target = dir.URLByAppendingPathComponent(filename) ?: return null
        val data = NSData.dataWithContentsOfURL(target) ?: return null
        val length = data.length.toInt()
        if (length <= 0) return ByteArray(0)
        val src = data.bytes ?: return null
        val out = ByteArray(length)
        out.usePinned { pinned ->
            memcpy(pinned.addressOf(0), src, data.length)
        }
        return out
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun deleteFile(dir: NSURL, filename: String) {
        dir.URLByAppendingPathComponent(filename)?.let { url ->
            NSFileManager.defaultManager.removeItemAtURL(url, error = null)
        }
    }

    private fun loadCurrent(): CustomSound? {
        val displayName = settings.getString(keyDisplayName)
        val filename = settings.getString(keyFilename)
        return if (displayName != null && filename != null) {
            CustomSound(displayName, filename)
        } else null
    }
}
