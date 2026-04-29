package intellij.kmm.settings.grind_track.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide dark/light mode preference.
 * null = follow the system setting (default on first launch).
 */
object ThemeState {
    private val _isDark = MutableStateFlow<Boolean?>(null)
    val isDark: StateFlow<Boolean?> = _isDark.asStateFlow()

    fun set(dark: Boolean) {
        _isDark.value = dark
    }
}
