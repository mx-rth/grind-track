package intellij.kmm.settings.grind_track.core.database

import androidx.room.TypeConverter
import kotlinx.datetime.DayOfWeek

class DayOfWeekTypeConverter {
    @TypeConverter
    fun fromString(value: String): Set<DayOfWeek> {
        if (value.isBlank()) return emptySet()
        return value.split(",").mapNotNull { name ->
            DayOfWeek.entries.firstOrNull { it.name == name }
        }.toSet()
    }

    @TypeConverter
    fun toString(days: Set<DayOfWeek>): String =
        days.joinToString(",") { it.name }
}
