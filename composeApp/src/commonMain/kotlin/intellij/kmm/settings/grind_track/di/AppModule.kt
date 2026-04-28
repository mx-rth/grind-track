package intellij.kmm.settings.grind_track.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import intellij.kmm.settings.grind_track.core.data.ExerciseRepository
import intellij.kmm.settings.grind_track.core.data.ProgressRepository
import intellij.kmm.settings.grind_track.core.data.RoutineRepository
import intellij.kmm.settings.grind_track.core.data.SeedDataManager
import intellij.kmm.settings.grind_track.core.data.WorkoutRepository
import intellij.kmm.settings.grind_track.core.database.DatabaseFactory
import intellij.kmm.settings.grind_track.core.database.GymTrackDatabase
import intellij.kmm.settings.grind_track.feature.progress.ui.ProgressViewModel
import intellij.kmm.settings.grind_track.feature.routines.ui.RoutineEditorViewModel
import intellij.kmm.settings.grind_track.feature.routines.ui.RoutinesViewModel
import intellij.kmm.settings.grind_track.feature.settings.ui.SettingsViewModel
import intellij.kmm.settings.grind_track.feature.workout.ui.WorkoutViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

val appModule: Module = module {
    single<GymTrackDatabase> {
        get<DatabaseFactory>().createBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<GymTrackDatabase>().exerciseDao() }
    single { get<GymTrackDatabase>().routineDao() }
    single { get<GymTrackDatabase>().routineExerciseDao() }
    single { get<GymTrackDatabase>().workoutSessionDao() }
    single { get<GymTrackDatabase>().setEntryDao() }

    single { RoutineRepository(get(), get(), get()) }
    single { ExerciseRepository(get()) }
    single { WorkoutRepository(get(), get()) }
    single { ProgressRepository(get(), get(), get()) }
    single { SeedDataManager(get(), get(), get(), get(), get()) }

    viewModel { RoutinesViewModel(get()) }
    viewModel { (routineId: Long) -> RoutineEditorViewModel(routineId, get(), get()) }
    viewModel { WorkoutViewModel(get(), get(), get()) }
    viewModel { ProgressViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}

expect fun platformModule(): Module

fun initKoin(config: KoinAppDeclaration? = null) {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return
    startKoin {
        config?.invoke(this)
        modules(appModule, platformModule())
    }
    CoroutineScope(Dispatchers.Default).launch {
        KoinPlatformTools.defaultContext().get().get<SeedDataManager>().seedIfEmpty()
    }
}
