package intellij.kmm.settings.grind_track.di

import intellij.kmm.settings.grind_track.core.database.DatabaseFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseFactory() }
}
