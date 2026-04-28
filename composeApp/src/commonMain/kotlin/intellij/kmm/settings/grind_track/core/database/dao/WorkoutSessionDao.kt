package intellij.kmm.settings.grind_track.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import intellij.kmm.settings.grind_track.core.database.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_session ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: WorkoutSession): Long
}
