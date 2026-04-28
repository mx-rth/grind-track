package intellij.kmm.settings.grind_track.feature.settings.ui

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val WAV_MIME_TYPES = arrayOf("audio/wav", "audio/x-wav", "audio/wave")

@Composable
actual fun rememberSoundFilePicker(
    onPicked: (PickedSoundFile) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val pendingUri = remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingUri.value = uri
    }

    LaunchedEffect(pendingUri.value) {
        val uri = pendingUri.value ?: return@LaunchedEffect
        val resolver = context.contentResolver
        val displayName = queryDisplayName(resolver, uri) ?: "custom_sound.wav"
        val bytes = withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        }
        pendingUri.value = null
        if (bytes != null) onPicked(PickedSoundFile(displayName, bytes))
    }

    return remember(launcher) { { launcher.launch(WAV_MIME_TYPES) } }
}

private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? =
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) cursor.getString(idx) else null
        } else null
    }
