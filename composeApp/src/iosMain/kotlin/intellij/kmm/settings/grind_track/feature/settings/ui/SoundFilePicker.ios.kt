package intellij.kmm.settings.grind_track.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberSoundFilePicker(
    onPicked: (PickedSoundFile) -> Unit,
): () -> Unit {
    val rootController = LocalUIViewController.current
    val onPickedRef = rememberUpdatedState(onPicked)

    val delegate = remember {
        SoundPickerDelegate { url ->
            val bytes = url.toByteArray() ?: return@SoundPickerDelegate
            val displayName = url.lastPathComponent ?: "custom_sound.wav"
            onPickedRef.value(PickedSoundFile(displayName, bytes))
        }
    }

    DisposableEffect(delegate) {
        onDispose { /* delegate held by remember; cleanup not strictly needed */ }
    }

    return remember(rootController, delegate) {
        {
            val wavType = UTType.typeWithFilenameExtension("wav")
            val types = listOfNotNull(wavType)
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = types)
            picker.delegate = delegate
            rootController.presentViewController(picker, animated = true, completion = null)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSURL.toByteArray(): ByteArray? {
    val accessing = startAccessingSecurityScopedResource()
    return try {
        val data = NSData.dataWithContentsOfURL(this) ?: return null
        val length = data.length.toInt()
        if (length == 0) return ByteArray(0)
        val ptr = data.bytes?.reinterpret<ByteVar>() ?: return null
        ptr.readBytes(length)
    } finally {
        if (accessing) stopAccessingSecurityScopedResource()
    }
}

private class SoundPickerDelegate(
    private val onUrl: (NSURL) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        onUrl(url)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        /* no-op */
    }
}
