package intellij.kmm.settings.grind_track.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import intellij.kmm.settings.grind_track.core.data.RoutineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatformTools

/**
 * Re-arms routine reminder alarms after device reboot or timezone change.
 * AlarmManager pending intents are cleared on reboot; iOS handles this natively.
 */
class RoutineReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_TIMEZONE_CHANGED) return

        val pending = goAsync()
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.Default) {
            try {
                val koin = KoinPlatformTools.defaultContext().getOrNull() ?: return@launch
                val repository = koin.get<RoutineRepository>()
                val scheduler = koin.get<RoutineNotificationScheduler>()
                val routines = repository.observeRoutines().first()
                scheduler.syncAll(routines)
            } finally {
                pending.finish()
            }
        }
    }
}
