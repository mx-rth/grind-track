package intellij.kmm.settings.grind_track

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform