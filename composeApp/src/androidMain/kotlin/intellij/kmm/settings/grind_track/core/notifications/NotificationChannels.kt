package intellij.kmm.settings.grind_track.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.content.getSystemService

internal const val REST_TIMER_CHANNEL_ID = "rest_timer_alarm"

/** Create the rest-timer notification channel. Idempotent. No-op below API 26. */
fun ensureRestTimerChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService<NotificationManager>() ?: return
    if (manager.getNotificationChannel(REST_TIMER_CHANNEL_ID) != null) return

    val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

    val channel = NotificationChannel(
        REST_TIMER_CHANNEL_ID,
        "Rest timer",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Plays an alarm when a workout rest interval ends."
        setSound(alarmSound, audioAttributes)
        enableVibration(true)
        vibrationPattern = longArrayOf(0L, 400L, 250L, 400L)
        setBypassDnd(false)
    }
    manager.createNotificationChannel(channel)
}
