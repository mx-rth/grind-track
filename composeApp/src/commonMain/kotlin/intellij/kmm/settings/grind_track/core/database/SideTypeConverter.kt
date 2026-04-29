package intellij.kmm.settings.grind_track.core.database

import androidx.room.TypeConverter
import intellij.kmm.settings.grind_track.core.database.entity.Side

class SideTypeConverter {
    @TypeConverter
    fun fromString(value: String?): Side? =
        value?.let { name -> Side.entries.firstOrNull { it.name == name } }

    @TypeConverter
    fun toString(side: Side?): String? = side?.name
}
