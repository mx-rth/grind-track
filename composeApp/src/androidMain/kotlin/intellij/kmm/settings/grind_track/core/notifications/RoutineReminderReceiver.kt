package intellij.kmm.settings.grind_track.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import intellij.kmm.settings.grind_track.MainActivity
import intellij.kmm.settings.grind_track.R
import intellij.kmm.settings.grind_track.core.data.RoutineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatformTools

class RoutineReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ROUTINE_REMINDER) return
        val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
        val dayIso = intent.getIntExtra(EXTRA_DAY_OF_WEEK, -1)
        if (routineId <= 0 || dayIso <= 0) return
        val day = isoDayNumberToDayOfWeek(dayIso) ?: return

        ensureRoutineReminderChannel(context)

        val pending = goAsync()
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.Default) {
            try {
                val koin = KoinPlatformTools.defaultContext().getOrNull() ?: return@launch
                val repository = koin.get<RoutineRepository>()
                val scheduler = koin.get<RoutineNotificationScheduler>()
                val routine = repository.observeRoutine(routineId).first()
                if (routine != null && routine.notificationEnabled &&
                    day in routine.scheduledDays && routine.notificationMinuteOfDay != null
                ) {
                    showNotification(context, routineId, routine.name)
                    scheduler.rearm(routineId, day, routine.notificationMinuteOfDay)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, routineId: Long, routineName: String) {
        val launchPendingIntent = PendingIntent.getActivity(
            context,
            routineId.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, ROUTINE_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(routineName.ifBlank { "Workout reminder" })
            .setContentText("Time to work out")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(launchPendingIntent)
            .build()

        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            NotificationManagerCompat.from(context)
                .notify(notificationId(routineId), notification)
        }
    }

    private fun notificationId(routineId: Long): Int = (0x7000 xor routineId.toInt())
}
