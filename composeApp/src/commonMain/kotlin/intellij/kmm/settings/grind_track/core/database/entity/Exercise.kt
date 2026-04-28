package intellij.kmm.settings.grind_track.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val notes: String = "",
    val defaultRestSeconds: Int = 90,
)
