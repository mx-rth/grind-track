package intellij.kmm.settings.grind_track.core.database.entity

enum class Side {
    LEFT,
    RIGHT;

    fun other(): Side = if (this == LEFT) RIGHT else LEFT
}
