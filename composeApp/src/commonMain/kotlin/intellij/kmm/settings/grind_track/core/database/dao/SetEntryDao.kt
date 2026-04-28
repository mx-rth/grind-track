package intellij.kmm.settings.grind_track.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import intellij.kmm.settings.grind_track.core.database.entity.SetEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SetEntryDao {
    @Query("SELECT * FROM set_entry WHERE sessionId = :sessionId ORDER BY setIndex ASC")
    fun observeForSession(sessionId: Long): Flow<List<SetEntry>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(setEntry: SetEntry): Long
}
