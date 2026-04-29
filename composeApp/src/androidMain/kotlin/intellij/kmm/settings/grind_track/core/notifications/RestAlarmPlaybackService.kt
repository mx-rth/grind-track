package intellij.kmm.settings.grind_track.core.notifications

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import intellij.kmm.settings.grind_track.MainActivity
import intellij.kmm.settings.grind_track.R

private const val NOTIFICATION_ID = 0x6712
private const val MAX_PLAYBACK_MILLIS = 60_000L

internal const val ACTION_START_PLAYBACK =
    "intellij.kmm.settings.grind_track.action.START_ALARM_PLAYBACK"
internal const val ACTION_STOP_PLAYBACK =
    "intellij.kmm.settings.grind_track.action.STOP_ALARM_PLAYBACK"
internal const val EXTRA_SOUND_URI = "soundUri"
internal const val EXTRA_CHANNEL_ID_FOR_SERVICE = "serviceChannelId"
internal const val EXTRA_BODY = "body"

/**
 * Foreground service that plays a user-supplied alarm sound in a continuous loop on the
 * `USAGE_ALARM` stream. Used for the stage-2 follow-up alarm when a custom alarm sound is
 * installed — channel-driven playback only fires once for content URIs, so we play
 * manually here. Auto-stops after [MAX_PLAYBACK_MILLIS] or when the notification is
 * dismissed (which fires [ACTION_STOP_PLAYBACK]).
 */
class RestAlarmPlaybackService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val stopHandler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stopSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_PLAYBACK -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_PLAYBACK, null -> Unit
        }

        val soundUri = intent?.getStringExtra(EXTRA_SOUND_URI)?.let(Uri::parse)
        val channelId = intent?.getStringExtra(EXTRA_CHANNEL_ID_FOR_SERVICE)
            ?: REST_TIMER_CHANNEL_ID
        val body = intent?.getStringExtra(EXTRA_BODY) ?: "Time for the next set"

        ensureRestTimerChannel(this)
        startInForeground(channelId, body)

        if (soundUri != null) startPlayback(soundUri)

        stopHandler.removeCallbacks(stopRunnable)
        stopHandler.postDelayed(stopRunnable, MAX_PLAYBACK_MILLIS)

        return START_NOT_STICKY
    }

    private fun startInForeground(channelId: String, body: String) {
        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val deleteIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, RestAlarmPlaybackService::class.java).setAction(ACTION_STOP_PLAYBACK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("Rest complete")
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(launchIntent)
            .setDeleteIntent(deleteIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startPlayback(uri: Uri) {
        runCatching {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@RestAlarmPlaybackService, uri)
                isLooping = true
                prepare()
                start()
            }
        }.onFailure {
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopHandler.removeCallbacks(stopRunnable)
        mediaPlayer?.let { player ->
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.release() }
        }
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onTimeout(startId: Int) {
        // Android 14+ short-service timeout (180s). We self-stop earlier via stopHandler;
        // this is a backstop.
        stopSelf()
    }
}
