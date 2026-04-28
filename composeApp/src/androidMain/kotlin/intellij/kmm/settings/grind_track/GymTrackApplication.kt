package intellij.kmm.settings.grind_track

import android.app.Application
import intellij.kmm.settings.grind_track.core.notifications.ensureRestTimerChannel
import intellij.kmm.settings.grind_track.di.initKoin
import org.koin.android.ext.koin.androidContext

class GymTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@GymTrackApplication)
        }
        ensureRestTimerChannel(this)
    }
}
