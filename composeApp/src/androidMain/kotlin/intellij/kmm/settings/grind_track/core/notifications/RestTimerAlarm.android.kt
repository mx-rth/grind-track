package intellij.kmm.settings.grind_track.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService

private const val REQUEST_CODE = 0x6711
internal const val ACTION_REST_DONE = "intellij.kmm.settings.grind_track.action.REST_DONE"
internal const val EXTRA_EXERCISE_NAME = "exerciseName"

actual class RestTimerAlarm(private val context: Context) {
    private val alarmManager: AlarmManager? = context.getSystemService<AlarmManager>()

    actual fun schedule(seconds: Int, exerciseName: String) {
        val manager = alarmManager ?: return
        ensureRestTimerChannel(context)
        val triggerAt = System.currentTimeMillis() + seconds * 1000L
        val pi = createPendingIntent(exerciseName)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (canExact) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    actual fun cancel() {
        val manager = alarmManager ?: return
        val pi = lookupPendingIntent() ?: return
        manager.cancel(pi)
        pi.cancel()
    }

    private fun createPendingIntent(exerciseName: String): PendingIntent {
        val intent = Intent(context, RestTimerReceiver::class.java).apply {
            action = ACTION_REST_DONE
            putExtra(EXTRA_EXERCISE_NAME, exerciseName)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun lookupPendingIntent(): PendingIntent? {
        val intent = Intent(context, RestTimerReceiver::class.java).setAction(ACTION_REST_DONE)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
