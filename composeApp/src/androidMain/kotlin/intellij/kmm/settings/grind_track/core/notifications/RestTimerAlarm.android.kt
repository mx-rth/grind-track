package intellij.kmm.settings.grind_track.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService

private const val REQUEST_CODE_INITIAL = 0x6711
private const val REQUEST_CODE_FOLLOWUP = 0x6712
private const val REQUEST_CODE_NOTIFICATION_ONLY = 0x6713
private const val FOLLOWUP_DELAY_SECONDS = 15

internal const val ACTION_REST_DONE = "intellij.kmm.settings.grind_track.action.REST_DONE"
internal const val ACTION_REST_FOLLOWUP_DONE =
    "intellij.kmm.settings.grind_track.action.REST_FOLLOWUP_DONE"
internal const val ACTION_COUNTDOWN_DONE =
    "intellij.kmm.settings.grind_track.action.COUNTDOWN_DONE"
internal const val EXTRA_EXERCISE_NAME = "exerciseName"
internal const val EXTRA_CHANNEL_ID = "channelId"

actual class RestTimerAlarm(
    private val context: Context,
    private val notificationSound: CustomSoundManager,
    private val alarmSound: CustomSoundManager,
) {
    private val alarmManager: AlarmManager? = context.getSystemService<AlarmManager>()

    actual fun schedule(seconds: Int, exerciseName: String) {
        val manager = alarmManager ?: return
        ensureRestTimerChannel(context)
        ensureRestTimerCompleteChannel(context)
        // Re-create the custom channels if missing (recovers from app-reinstall edge cases
        // where preferences persisted but channels were wiped) and re-grant FileProvider
        // URI permissions to the system UI process.
        notificationSound.ensureChannel()
        alarmSound.ensureChannel()

        val now = System.currentTimeMillis()
        val initialTriggerAt = now + seconds * 1000L
        val followupTriggerAt = now + (seconds + FOLLOWUP_DELAY_SECONDS) * 1000L
        // Stage 1 uses the user's custom notification sound when installed, otherwise
        // the default chime. Stage 2 uses the user's custom alarm sound when installed,
        // otherwise the default alarm tone.
        val initialChannelId = notificationSound.currentChannelId()
        val followupChannelId = alarmSound.currentChannelId()

        val initialPi = createPendingIntent(
            requestCode = REQUEST_CODE_INITIAL,
            action = ACTION_REST_DONE,
            exerciseName = exerciseName,
            channelId = initialChannelId,
        )
        val followupPi = createPendingIntent(
            requestCode = REQUEST_CODE_FOLLOWUP,
            action = ACTION_REST_FOLLOWUP_DONE,
            exerciseName = exerciseName,
            channelId = followupChannelId,
        )

        scheduleExact(manager, initialTriggerAt, initialPi)
        scheduleExact(manager, followupTriggerAt, followupPi)
    }

    actual fun scheduleNotificationOnly(seconds: Int, exerciseName: String) {
        val manager = alarmManager ?: return
        ensureRestTimerCompleteChannel(context)
        val triggerAt = System.currentTimeMillis() + seconds * 1000L
        val pi = createPendingIntent(
            requestCode = REQUEST_CODE_NOTIFICATION_ONLY,
            action = ACTION_COUNTDOWN_DONE,
            exerciseName = exerciseName,
            channelId = REST_TIMER_COMPLETE_CHANNEL_ID,
        )
        scheduleExact(manager, triggerAt, pi)
    }

    actual fun cancel() {
        val manager = alarmManager ?: return
        cancelOne(manager, REQUEST_CODE_INITIAL, ACTION_REST_DONE)
        cancelOne(manager, REQUEST_CODE_FOLLOWUP, ACTION_REST_FOLLOWUP_DONE)
        cancelOne(manager, REQUEST_CODE_NOTIFICATION_ONLY, ACTION_COUNTDOWN_DONE)
        // Stop the looping playback service if a custom alarm is currently sounding.
        // runCatching guards against background-start restrictions when called from a
        // non-foreground context (alarms cancelled while the app isn't visible).
        val stopIntent = Intent(context, RestAlarmPlaybackService::class.java)
            .setAction(ACTION_STOP_PLAYBACK)
        runCatching { context.startService(stopIntent) }
    }

    private fun scheduleExact(manager: AlarmManager, triggerAt: Long, pi: PendingIntent) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (canExact) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelOne(manager: AlarmManager, requestCode: Int, action: String) {
        val intent = Intent(context, RestTimerReceiver::class.java).setAction(action)
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        manager.cancel(pi)
        pi.cancel()
    }

    private fun createPendingIntent(
        requestCode: Int,
        action: String,
        exerciseName: String,
        channelId: String,
    ): PendingIntent {
        val intent = Intent(context, RestTimerReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_EXERCISE_NAME, exerciseName)
            putExtra(EXTRA_CHANNEL_ID, channelId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
