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
internal const val REST_TIMER_COMPLETE_CHANNEL_ID = "rest_timer_complete"
internal const val ROUTINE_REMINDER_CHANNEL_ID = "routine_reminders"

private val CustomSoundKind.channelPrefix: String
    get() = when (this) {
        CustomSoundKind.Notification -> "rest_timer_complete_custom_"
        CustomSoundKind.Alarm -> "rest_timer_alarm_custom_"
    }

internal fun customChannelId(kind: CustomSoundKind, generation: Int): String =
    "${kind.channelPrefix}$generation"

private fun alarmAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ALARM)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

private fun notificationAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
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
 * Create a rest-timer channel that plays a user-supplied sound URI. The audio attributes
 * and importance follow the channel's [kind]: Notification → notification volume + default
 * importance (stage-1 chime); Alarm → alarm volume + high importance (stage-2 alarm tone,
 * looping on most devices). Channel sounds are immutable post-creation, so a fresh ID is
 * used on each install (caller increments [generation]). Idempotent for a given
 * (kind, generation) pair. No-op below API 26.
 */
fun ensureCustomChannel(
    context: Context,
    kind: CustomSoundKind,
    soundUri: Uri,
    generation: Int,
): String {
    val id = customChannelId(kind, generation)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return id
    val manager = context.getSystemService<NotificationManager>() ?: return id

    // Grant the system UI persistent read access to the FileProvider URI.
    context.grantUriPermission(
        "com.android.systemui",
        soundUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION,
    )

    if (manager.getNotificationChannel(id) != null) return id

    val template = when (kind) {
        CustomSoundKind.Notification -> ChannelTemplate(
            name = "Rest complete (custom sound)",
            description = "Plays your selected sound when a workout rest interval ends.",
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            attrs = notificationAudioAttributes(),
            // Notification kind: channel itself plays the sound (single play).
            channelSound = soundUri,
        )
        CustomSoundKind.Alarm -> ChannelTemplate(
            name = "Rest alarm (custom sound)",
            description = "Plays your selected sound 15 seconds after the rest interval ends.",
            importance = NotificationManager.IMPORTANCE_HIGH,
            attrs = alarmAudioAttributes(),
            // Alarm kind: channel is silent — RestAlarmPlaybackService plays the sound
            // looped on the alarm stream so it behaves like a real alarm rather than a
            // single-shot notification chime.
            channelSound = null,
        )
    }

    val channel = NotificationChannel(id, template.name, template.importance).apply {
        this.description = template.description
        setSound(template.channelSound, template.attrs)
        enableVibration(true)
        if (kind == CustomSoundKind.Alarm) {
            vibrationPattern = longArrayOf(0L, 400L, 250L, 400L)
            setBypassDnd(false)
        }
    }
    manager.createNotificationChannel(channel)
    return id
}

fun deleteCustomChannel(context: Context, kind: CustomSoundKind, generation: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService<NotificationManager>() ?: return
    manager.deleteNotificationChannel(customChannelId(kind, generation))
}

private data class ChannelTemplate(
    val name: String,
    val description: String,
    val importance: Int,
    val attrs: AudioAttributes,
    val channelSound: Uri?,
)

/**
 * Gentle "rest complete" channel. Default importance, default notification sound, plays
 * once. Used for the initial rest-end notification; the follow-up alarm 15 s later goes
 * on the alarm-style channel instead.
 */
fun ensureRestTimerCompleteChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService<NotificationManager>() ?: return
    if (manager.getNotificationChannel(REST_TIMER_COMPLETE_CHANNEL_ID) != null) return

    val channel = NotificationChannel(
        REST_TIMER_COMPLETE_CHANNEL_ID,
        "Rest complete",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Plays a chime when a workout rest interval ends."
        enableVibration(true)
    }
    manager.createNotificationChannel(channel)
}

/** Channel for the per-routine workout reminder. Default importance, default sound. */
fun ensureRoutineReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService<NotificationManager>() ?: return
    if (manager.getNotificationChannel(ROUTINE_REMINDER_CHANNEL_ID) != null) return

    val channel = NotificationChannel(
        ROUTINE_REMINDER_CHANNEL_ID,
        "Routine reminders",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Reminders to start your scheduled workouts."
        enableVibration(true)
    }
    manager.createNotificationChannel(channel)
}
