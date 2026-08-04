package intellij.kmm.settings.demobla

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform