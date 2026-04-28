package intellij.kmm.settings.grind_track.di

import intellij.kmm.settings.grind_track.core.database.DatabaseFactory
import intellij.kmm.settings.grind_track.core.notifications.CustomSoundManager
import intellij.kmm.settings.grind_track.core.notifications.RestTimerAlarm
import intellij.kmm.settings.grind_track.core.notifications.RoutineNotificationScheduler
import intellij.kmm.settings.grind_track.core.preferences.SettingsStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseFactory(androidContext()) }
    single { SettingsStore(androidContext()) }
    single { CustomSoundManager(androidContext(), get()) }
    single { RestTimerAlarm(androidContext(), get()) }
    single { RoutineNotificationScheduler(androidContext()) }
}
