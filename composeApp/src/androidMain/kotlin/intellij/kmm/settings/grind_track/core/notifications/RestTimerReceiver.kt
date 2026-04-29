package intellij.kmm.settings.grind_track.core.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import intellij.kmm.settings.grind_track.MainActivity
import intellij.kmm.settings.grind_track.R
import intellij.kmm.settings.grind_track.di.CUSTOM_SOUND_ALARM
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatformTools

private const val NOTIFICATION_ID_INITIAL = 0x6711
private const val NOTIFICATION_ID_FOLLOWUP = 0x6712
private const val NOTIFICATION_ID_COUNTDOWN = 0x6713

class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isInitial = intent.action == ACTION_REST_DONE
        val isFollowup = intent.action == ACTION_REST_FOLLOWUP_DONE
        val isCountdown = intent.action == ACTION_COUNTDOWN_DONE
        if (!isInitial && !isFollowup && !isCountdown) return

        if (isFollowup) ensureRestTimerChannel(context) else ensureRestTimerCompleteChannel(context)

        val exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME).orEmpty()
        val defaultChannelId = if (isFollowup) REST_TIMER_CHANNEL_ID else REST_TIMER_COMPLETE_CHANNEL_ID
        val requestedChannelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: defaultChannelId
        // Fall back to the default channel if the requested (custom) channel no longer
        // exists — without this the system silently drops the notification.
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            requestedChannelId != defaultChannelId &&
            context.getSystemService<NotificationManager>()?.getNotificationChannel(requestedChannelId) == null
        ) {
            defaultChannelId
        } else {
            requestedChannelId
        }
        val title = if (isCountdown) "Time's up" else "Rest complete"
        val body = if (isCountdown) {
            if (exerciseName.isNotBlank()) "$exerciseName complete" else "Countdown complete"
        } else if (exerciseName.isNotBlank()) {
            "Time for $exerciseName"
        } else {
            "Time for the next set"
        }

        // Stage 2 with a custom alarm sound: delegate to the foreground service so the
        // sound plays in a continuous loop on USAGE_ALARM (channel-driven playback only
        // fires once for content URIs). The service handles the foreground notification.
        if (isFollowup) {
            val koin = KoinPlatformTools.defaultContext().getOrNull()
            val alarmManager = koin?.get<CustomSoundManager>(named(CUSTOM_SOUND_ALARM))
            val customUri = alarmManager?.currentSoundUri()
            if (customUri != null) {
                val serviceIntent = Intent(context, RestAlarmPlaybackService::class.java).apply {
                    action = ACTION_START_PLAYBACK
                    putExtra(EXTRA_SOUND_URI, customUri.toString())
                    putExtra(EXTRA_CHANNEL_ID_FOR_SERVICE, channelId)
                    putExtra(EXTRA_BODY, body)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
                return
            }
        }

        val notificationId = when {
            isFollowup -> NOTIFICATION_ID_FOLLOWUP
            isCountdown -> NOTIFICATION_ID_COUNTDOWN
            else -> NOTIFICATION_ID_INITIAL
        }
        val category = if (isFollowup) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER
        val priority = if (isFollowup) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT

        val launchPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(category)
            .setPriority(priority)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(launchPendingIntent)
            .build()

        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
