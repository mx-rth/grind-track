package intellij.kmm.settings.grind_track.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import intellij.kmm.settings.grind_track.core.notifications.CustomSound
import intellij.kmm.settings.grind_track.core.notifications.CustomSoundManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val currentSound: CustomSound? = null,
)

class SettingsViewModel(
    private val customSoundManager: CustomSoundManager,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = customSoundManager.updates
        .map { SettingsUiState(currentSound = it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsUiState(currentSound = customSoundManager.current()),
        )

    fun installSound(picked: PickedSoundFile) {
        viewModelScope.launch {
            customSoundManager.install(picked.displayName, picked.bytes)
        }
    }

    fun resetToDefault() {
        customSoundManager.uninstall()
    }
}
