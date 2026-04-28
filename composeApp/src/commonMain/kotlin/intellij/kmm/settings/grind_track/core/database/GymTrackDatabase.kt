package intellij.kmm.settings.grind_track.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import intellij.kmm.settings.grind_track.core.database.dao.ExerciseDao
import intellij.kmm.settings.grind_track.core.database.dao.RoutineDao
import intellij.kmm.settings.grind_track.core.database.dao.RoutineExerciseDao
import intellij.kmm.settings.grind_track.core.database.dao.SetEntryDao
import intellij.kmm.settings.grind_track.core.database.dao.WorkoutSessionDao
import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import intellij.kmm.settings.grind_track.core.database.entity.RoutineExercise
import intellij.kmm.settings.grind_track.core.database.entity.SetEntry
import intellij.kmm.settings.grind_track.core.database.entity.WorkoutSession

@Database(
    entities = [
        Exercise::class,
        Routine::class,
        RoutineExercise::class,
        WorkoutSession::class,
        SetEntry::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(InstantTypeConverter::class)
@ConstructedBy(GymTrackDatabaseConstructor::class)
abstract class GymTrackDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineExerciseDao(): RoutineExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun setEntryDao(): SetEntryDao

    companion object {
        const val DATABASE_NAME = "gymtrack.db"
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object GymTrackDatabaseConstructor : RoomDatabaseConstructor<GymTrackDatabase> {
    override fun initialize(): GymTrackDatabase
}
