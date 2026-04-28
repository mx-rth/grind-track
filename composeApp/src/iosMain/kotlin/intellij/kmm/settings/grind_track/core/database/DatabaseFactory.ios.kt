package intellij.kmm.settings.grind_track.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual class DatabaseFactory {
    @OptIn(ExperimentalForeignApi::class)
    actual fun createBuilder(): RoomDatabase.Builder<GymTrackDatabase> {
        val documentDirectory: NSURL = requireNotNull(
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            )
        )
        val dbPath = requireNotNull(documentDirectory.path) +
            "/" + GymTrackDatabase.DATABASE_NAME
        return Room.databaseBuilder<GymTrackDatabase>(name = dbPath)
    }
}
