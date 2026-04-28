package intellij.kmm.settings.grind_track.core.preferences

expect class SettingsStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getInt(key: String, default: Int = 0): Int
    fun putInt(key: String, value: Int)
}
