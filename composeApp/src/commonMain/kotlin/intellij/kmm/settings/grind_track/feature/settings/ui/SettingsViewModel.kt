package intellij.kmm.settings.grind_track.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import intellij.kmm.settings.grind_track.core.designsystem.MascotVariant
import intellij.kmm.settings.grind_track.core.notifications.CustomSound
import intellij.kmm.settings.grind_track.core.notifications.CustomSoundManager
import intellij.kmm.settings.grind_track.core.preferences.MascotPreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationSound: CustomSound? = null,
    val alarmSound: CustomSound? = null,
    val mascotVariant: MascotVariant = MascotVariant.Female,
)

class SettingsViewModel(
    private val notificationSoundManager: CustomSoundManager,
    private val alarmSoundManager: CustomSoundManager,
    private val mascotPreference: MascotPreference,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(
        notificationSoundManager.updates,
        alarmSoundManager.updates,
        mascotPreference.variant,
    ) { notification, alarm, mascot ->
        SettingsUiState(
            notificationSound = notification,
            alarmSound = alarm,
            mascotVariant = mascot,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(
            notificationSound = notificationSoundManager.current(),
            alarmSound = alarmSoundManager.current(),
            mascotVariant = mascotPreference.variant.value,
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

    fun selectMascot(variant: MascotVariant) {
        mascotPreference.set(variant)
    }
}
