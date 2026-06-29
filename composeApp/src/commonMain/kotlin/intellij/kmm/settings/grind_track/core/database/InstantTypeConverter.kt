package intellij.kmm.settings.grind_track.core.database

import androidx.room.TypeConverter
import kotlin.time.Instant

class InstantTypeConverter {
    @TypeConverter
    fun fromEpochMillis(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun toEpochMillis(instant: Instant?): Long? = instant?.toEpochMilliseconds()
}
