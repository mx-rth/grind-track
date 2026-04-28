package intellij.kmm.settings.grind_track.core.database

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun createBuilder(): RoomDatabase.Builder<GymTrackDatabase>
}
