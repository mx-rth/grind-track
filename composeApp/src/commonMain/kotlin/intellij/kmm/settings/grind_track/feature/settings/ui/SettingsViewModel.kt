package intellij.kmm.settings.grind_track.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import intellij.kmm.settings.grind_track.core.notifications.CustomSound
import intellij.kmm.settings.grind_track.core.notifications.CustomSoundManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationSound: CustomSound? = null,
    val alarmSound: CustomSound? = null,
)

class SettingsViewModel(
    private val notificationSoundManager: CustomSoundManager,
    private val alarmSoundManager: CustomSoundManager,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(
        notificationSoundManager.updates,
        alarmSoundManager.updates,
    ) { notification, alarm ->
        SettingsUiState(notificationSound = notification, alarmSound = alarm)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(
            notificationSound = notificationSoundManager.current(),
            alarmSound = alarmSoundManager.current(),
        ),
    )

    fun installNotificationSound(picked: PickedSoundFile) {
        viewModelScope.launch {
            notificationSoundManager.install(picked.displayName, picked.bytes)
        }
    }

    fun resetNotificationSound() {
        notificationSoundManager.uninstall()
    }

    fun installAlarmSound(picked: PickedSoundFile) {
        viewModelScope.launch {
            alarmSoundManager.install(picked.displayName, picked.bytes)
        }
    }

    fun resetAlarmSound() {
        alarmSoundManager.uninstall()
    }
}
