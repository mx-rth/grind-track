package intellij.kmm.settings.grind_track.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.content.getSystemService

internal const val REST_TIMER_CHANNEL_ID = "rest_timer_alarm"
internal const val REST_TIMER_CUSTOM_CHANNEL_PREFIX = "rest_timer_alarm_custom_"

internal fun customRestTimerChannelId(generation: Int): String =
    "$REST_TIMER_CUSTOM_CHANNEL_PREFIX$generation"

private fun alarmAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ALARM)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

/** Create the rest-timer notification channel. Idempotent. No-op below API 26. */
fun ensureRestTimerChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService<NotificationManager>() ?: return
    if (manager.getNotificationChannel(REST_TIMER_CHANNEL_ID) != null) return

    val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    val channel = NotificationChannel(
        REST_TIMER_CHANNEL_ID,
        "Rest timer",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Plays an alarm when a workout rest interval ends."
        setSound(alarmSound, alarmAudioAttributes())
        enableVibration(true)
        vibrationPattern = longArrayOf(0L, 400L, 250L, 400L)
        setBypassDnd(false)
    }
    manager.createNotificationChannel(channel)
}

/**
 * Create a rest-timer channel that plays a user-supplied sound URI. Channel sounds are
 * immutable post-creation, so a fresh ID is used on each install (caller increments
 * [generation]). Idempotent for a given [generation]. No-op below API 26.
 */
fun ensureCustomRestTimerChannel(context: Context, soundUri: Uri, generation: Int): String {
    val id = customRestTimerChannelId(generation)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return id
    val manager = context.getSystemService<NotificationManager>() ?: return id

    // Grant the system UI persistent read access to the FileProvider URI.
    context.grantUriPermission(
        "com.android.systemui",
        soundUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION,
    )

    if (manager.getNotificationChannel(id) != null) return id

    val channel = NotificationChannel(
        id,
        "Rest timer (custom sound)",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Plays your selected sound when a workout rest interval ends."
        setSound(soundUri, alarmAudioAttributes())
        enableVibration(true)
        vibrationPattern = longArrayOf(0L, 400L, 250L, 400L)
        setBypassDnd(false)
    }
    manager.createNotificationChannel(channel)
    return id
}

fun deleteCustomRestTimerChannel(context: Context, generation: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService<NotificationManager>() ?: return
    manager.deleteNotificationChannel(customRestTimerChannelId(generation))
}
