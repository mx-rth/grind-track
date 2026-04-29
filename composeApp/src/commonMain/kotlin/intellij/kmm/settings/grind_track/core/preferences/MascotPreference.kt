package intellij.kmm.settings.grind_track.core.preferences

import intellij.kmm.settings.grind_track.core.designsystem.MascotVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val KEY = "mascot_variant"

/** Persistent user preference for which mascot variant to show throughout the app. */
class MascotPreference(private val store: SettingsStore) {
    private val _variant = MutableStateFlow(load())
    val variant: StateFlow<MascotVariant> = _variant.asStateFlow()

    fun set(variant: MascotVariant) {
        if (_variant.value == variant) return
        store.putString(KEY, variant.name)
        _variant.value = variant
    }

    private fun load(): MascotVariant {
        val name = store.getString(KEY) ?: return MascotVariant.Female
        return MascotVariant.entries.firstOrNull { it.name == name } ?: MascotVariant.Female
    }
}
