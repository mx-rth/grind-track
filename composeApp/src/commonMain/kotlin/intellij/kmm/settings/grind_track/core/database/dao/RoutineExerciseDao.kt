package intellij.kmm.settings.grind_track.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import intellij.kmm.settings.grind_track.core.database.entity.RoutineExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineExerciseDao {
    @Query("SELECT * FROM routine_exercise WHERE routineId = :routineId ORDER BY position ASC")
    fun observeForRoutine(routineId: Long): Flow<List<RoutineExercise>>

    @Query("SELECT * FROM routine_exercise WHERE routineId = :routineId ORDER BY position ASC")
    suspend fun listForRoutine(routineId: Long): List<RoutineExercise>

    @Query("SELECT * FROM routine_exercise WHERE id = :id")
    suspend fun getById(id: Long): RoutineExercise?

    @Query("SELECT COALESCE(MAX(position), -1) FROM routine_exercise WHERE routineId = :routineId")
    suspend fun maxPosition(routineId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(routineExercise: RoutineExercise): Long

    @Update
    suspend fun update(routineExercise: RoutineExercise)

    @Delete
    suspend fun delete(routineExercise: RoutineExercise)

    @Query("DELETE FROM routine_exercise WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    suspend fun swapPositions(a: RoutineExercise, b: RoutineExercise) {
        update(a.copy(position = b.position))
        update(b.copy(position = a.position))
    }
}
