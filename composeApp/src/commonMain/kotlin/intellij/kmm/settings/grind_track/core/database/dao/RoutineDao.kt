package intellij.kmm.settings.grind_track.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Routine>>

    @Query("SELECT * FROM routine WHERE id = :id")
    fun observeById(id: Long): Flow<Routine?>

    @Query("SELECT * FROM routine WHERE id = :id")
    suspend fun getById(id: Long): Routine?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(routine: Routine): Long

    @Update
    suspend fun update(routine: Routine)

    @Delete
    suspend fun delete(routine: Routine)

    @Query("DELETE FROM routine WHERE id = :id")
    suspend fun deleteById(id: Long)
}
