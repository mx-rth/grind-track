package intellij.kmm.settings.grind_track.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class DatabaseFactory(private val context: Context) {
    actual fun createBuilder(): RoomDatabase.Builder<GymTrackDatabase> {
        val dbFile = context.getDatabasePath(GymTrackDatabase.DATABASE_NAME)
        return Room.databaseBuilder<GymTrackDatabase>(
            context = context.applicationContext,
            name = dbFile.absolutePath,
        )
    }
}
