package intellij.kmm.settings.grind_track

import android.app.Application
import intellij.kmm.settings.grind_track.core.data.ProgressRepository
import intellij.kmm.settings.grind_track.core.notifications.ensureRestTimerChannel
import intellij.kmm.settings.grind_track.di.initKoin
import androidx.glance.appwidget.GlanceAppWidgetManager
import intellij.kmm.settings.grind_track.widget.WorkoutWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.mp.KoinPlatformTools

class GymTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@GymTrackApplication)
        }
        ensureRestTimerChannel(this)
        observeWidgetData()
    }

    private fun observeWidgetData() {
        val context = this
        CoroutineScope(Dispatchers.Default).launch {
            val repo = KoinPlatformTools.defaultContext().get().get<ProgressRepository>()
            repo.observeWidgetData().collect {
                val manager = GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(WorkoutWidget::class.java)
                val widget = WorkoutWidget()
                ids.forEach { id -> widget.update(context, id) }
            }
        }
    }
}
