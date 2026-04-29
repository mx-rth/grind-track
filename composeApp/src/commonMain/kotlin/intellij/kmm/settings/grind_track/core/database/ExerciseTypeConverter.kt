package intellij.kmm.settings.grind_track.core.database

import androidx.room.TypeConverter
import intellij.kmm.settings.grind_track.core.database.entity.ExerciseType

class ExerciseTypeConverter {
    @TypeConverter
    fun fromString(value: String): ExerciseType =
        ExerciseType.entries.firstOrNull { it.name == value } ?: ExerciseType.STRENGTH

    @TypeConverter
    fun toString(type: ExerciseType): String = type.name
}
