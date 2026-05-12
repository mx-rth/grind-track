package intellij.kmm.settings.grind_track.core.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private const val PREFS_NAME = "grind_track_settings"

actual class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    actual fun getString(key: String): String? = prefs.getString(key, null)

    actual fun putString(key: String, value: String?) {
        prefs.edit {
            if (value == null) remove(key) else putString(key, value)
        }
    }

    actual fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)

    actual fun putInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    actual fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)

    actual fun putFloat(key: String, value: Float) {
        prefs.edit { putFloat(key, value) }
    }
}
