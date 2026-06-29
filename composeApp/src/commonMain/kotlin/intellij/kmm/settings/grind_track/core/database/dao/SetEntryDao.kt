package intellij.kmm.settings.grind_track.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import intellij.kmm.settings.grind_track.core.database.entity.ExerciseType
import intellij.kmm.settings.grind_track.core.database.entity.SetEntry
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

data class SetEntryRow(
    val exerciseId: Long,
    val exerciseType: ExerciseType,
    val sessionId: Long,
    val weight: Double,
    val distanceMeters: Int?,
    val durationSeconds: Double?,
    val completedAt: Instant,
)

@Dao
interface SetEntryDao {
    @Query("SELECT * FROM set_entry WHERE sessionId = :sessionId ORDER BY setIndex ASC")
    fun observeForSession(sessionId: Long): Flow<List<SetEntry>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(setEntry: SetEntry): Long

    @Query(
        """
        SELECT e.* FROM exercise e
        WHERE EXISTS (
            SELECT 1 FROM set_entry se
            INNER JOIN routine_exercise re ON re.id = se.routineExerciseId
            WHERE re.exerciseId = e.id
        )
        ORDER BY (
            SELECT MAX(se.completedAt) FROM set_entry se
            INNER JOIN routine_exercise re ON re.id = se.routineExerciseId
            WHERE re.exerciseId = e.id
        ) DESC
        """
    )
    fun observeExercisesWithActivity(): Flow<List<Exercise>>

    @Query(
        """
        SELECT se.* FROM set_entry se
        INNER JOIN routine_exercise re ON re.id = se.routineExerciseId
        WHERE re.exerciseId = :exerciseId
        ORDER BY se.completedAt DESC
        """
    )
    fun observeHistoryForExercise(exerciseId: Long): Flow<List<SetEntry>>

    @Query(
        """
        SELECT re.exerciseId, e.type AS exerciseType, se.sessionId, se.weight,
               se.distanceMeters, se.durationSeconds, se.completedAt
        FROM set_entry se
        INNER JOIN routine_exercise re ON re.id = se.routineExerciseId
        INNER JOIN exercise e ON e.id = re.exerciseId
        ORDER BY se.completedAt ASC
        """
    )
    fun observeAllWithExerciseType(): Flow<List<SetEntryRow>>
}
