package intellij.kmm.settings.grind_track.core.preferences

import platform.Foundation.NSUserDefaults

actual class SettingsStore {
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String): String? = defaults.stringForKey(key)

    actual fun putString(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(key) else defaults.setObject(value, key)
    }

    actual fun getInt(key: String, default: Int): Int =
        if (defaults.objectForKey(key) == null) default else defaults.integerForKey(key).toInt()

    actual fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), key)
    }

    actual fun getFloat(key: String, default: Float): Float =
        if (defaults.objectForKey(key) == null) default else defaults.floatForKey(key)

    actual fun putFloat(key: String, value: Float) {
        defaults.setFloat(value, key)
    }
}
