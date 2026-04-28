package intellij.kmm.settings.grind_track.di

import intellij.kmm.settings.grind_track.core.database.DatabaseFactory
import intellij.kmm.settings.grind_track.core.notifications.CustomSoundManager
import intellij.kmm.settings.grind_track.core.notifications.RestTimerAlarm
import intellij.kmm.settings.grind_track.core.preferences.SettingsStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseFactory() }
    single { SettingsStore() }
    single { CustomSoundManager(get()) }
    single { RestTimerAlarm(get()) }
}
