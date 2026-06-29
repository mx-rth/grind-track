package intellij.kmm.settings.grind_track.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(
    tableName = "workout_session",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("routineId")],
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineId: Long?,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
)
