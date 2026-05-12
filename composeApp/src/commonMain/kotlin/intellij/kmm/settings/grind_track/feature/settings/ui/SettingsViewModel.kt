package intellij.kmm.settings.grind_track.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import intellij.kmm.settings.grind_track.core.data.SeedDataManager
import intellij.kmm.settings.grind_track.core.designsystem.MascotVariant
import intellij.kmm.settings.grind_track.core.notifications.CustomSound
import intellij.kmm.settings.grind_track.core.notifications.CustomSoundManager
import intellij.kmm.settings.grind_track.core.preferences.MascotPreference
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationSound: CustomSound? = null,
    val alarmSound: CustomSound? = null,
    val mascotVariant: MascotVariant = MascotVariant.Female,
    val notificationGainDb: Float = 0f,
    val alarmGainDb: Float = 0f,
)

class SettingsViewModel(
    private val notificationSoundManager: CustomSoundManager,
    private val alarmSoundManager: CustomSoundManager,
    private val mascotPreference: MascotPreference,
    private val seedDataManager: SeedDataManager,
) : ViewModel() {
    private val notificationGainInFlight =
        MutableStateFlow(notificationSoundManager.gainDb.value)
    private val alarmGainInFlight =
        MutableStateFlow(alarmSoundManager.gainDb.value)

    private val notificationGainPending = MutableSharedFlow<Float>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val alarmGainPending = MutableSharedFlow<Float>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val state: StateFlow<SettingsUiState> = combine(
        notificationSoundManager.updates,
        alarmSoundManager.updates,
        mascotPreference.variant,
        notificationGainInFlight,
        alarmGainInFlight,
    ) { notification, alarm, mascot, notifGain, alarmGain ->
        SettingsUiState(
            notificationSound = notification,
            alarmSound = alarm,
            mascotVariant = mascot,
            notificationGainDb = notifGain,
            alarmGainDb = alarmGain,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(
            notificationSound = notificationSoundManager.current(),
            alarmSound = alarmSoundManager.current(),
            mascotVariant = mascotPreference.variant.value,
            notificationGainDb = notificationSoundManager.gainDb.value,
            alarmGainDb = alarmSoundManager.gainDb.value,
        ),
    )

    init {
        // External resets (e.g. install/uninstall sets gain back to 0) sync into UI.
        viewModelScope.launch {
            notificationSoundManager.gainDb.collect { notificationGainInFlight.value = it }
        }
        viewModelScope.launch {
            alarmSoundManager.gainDb.collect { alarmGainInFlight.value = it }
        }
        // Debounce slider drags into one persist per kind.
        viewModelScope.launch {
            notificationGainPending
                .debounce(300.milliseconds)
                .distinctUntilChanged()
                .collect { notificationSoundManager.setGainDb(it) }
        }
        viewModelScope.launch {
            alarmGainPending
                .debounce(300.milliseconds)
                .distinctUntilChanged()
                .collect { alarmSoundManager.setGainDb(it) }
        }
    }

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

    fun setNotificationGainDb(db: Float) {
        val clamped = db.coerceIn(0f, 12f)
        notificationGainInFlight.value = clamped
        notificationGainPending.tryEmit(clamped.roundToInt().toFloat())
    }

    fun setAlarmGainDb(db: Float) {
        val clamped = db.coerceIn(0f, 12f)
        alarmGainInFlight.value = clamped
        alarmGainPending.tryEmit(clamped.roundToInt().toFloat())
    }

    fun selectMascot(variant: MascotVariant) {
        mascotPreference.set(variant)
    }

    fun seedMockData() {
        viewModelScope.launch {
            seedDataManager.seedIfEmpty()
        }
    }
}
