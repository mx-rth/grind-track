package intellij.kmm.settings.grind_track.core.notifications

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationInterruptionLevel
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

private const val IDENTIFIER_INITIAL = "rest_timer_alarm"
private const val IDENTIFIER_FOLLOWUP = "rest_timer_alarm_followup"
private const val FOLLOWUP_DELAY_SECONDS = 15

actual class RestTimerAlarm(
    private val notificationSound: CustomSoundManager,
    private val alarmSound: CustomSoundManager,
) {

    private val center: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter()
    private val foregroundDelegate = ForegroundDelegate()

    init {
        center.delegate = foregroundDelegate
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
        ) { _, _ -> /* result ignored */ }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun schedule(seconds: Int, exerciseName: String) {
        cancel()

        val title = "Rest complete"
        val body = if (exerciseName.isNotBlank()) "Time for $exerciseName" else "Time for the next set"

        // Initial: gentle chime at +seconds. Uses the user's custom notification sound
        // when one is installed; otherwise the system default notification sound.
        val notificationFilename = notificationSound.current()?.internalFilename
        val initialSound = if (notificationFilename != null) {
            UNNotificationSound.soundNamed(notificationFilename)
        } else {
            UNNotificationSound.defaultSound
        }
        val initialContent = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(initialSound)
        }
        val initialTrigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = seconds.toDouble().coerceAtLeast(1.0),
            repeats = false,
        )
        center.addNotificationRequest(
            UNNotificationRequest.requestWithIdentifier(
                identifier = IDENTIFIER_INITIAL,
                content = initialContent,
                trigger = initialTrigger,
            ),
        ) { _: NSError? -> /* result ignored */ }

        // Follow-up: time-sensitive notification at +seconds+15. The time-sensitive
        // interruption level breaks through Focus modes (Do Not Disturb, Sleep, etc.) so
        // the user is woken even when DND is on. It does NOT bypass the physical mute
        // switch — only Critical Alerts do that, which is gated behind Apple approval.
        // Uses the user's custom alarm sound when installed; otherwise the default sound.
        val alarmFilename = alarmSound.current()?.internalFilename
        val followupSound = if (alarmFilename != null) {
            UNNotificationSound.soundNamed(alarmFilename)
        } else {
            UNNotificationSound.defaultSound
        }
        val followupContent = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(followupSound)
            setInterruptionLevel(UNNotificationInterruptionLevel.UNNotificationInterruptionLevelTimeSensitive)
        }
        val followupTrigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = (seconds + FOLLOWUP_DELAY_SECONDS).toDouble().coerceAtLeast(1.0),
            repeats = false,
        )
        center.addNotificationRequest(
            UNNotificationRequest.requestWithIdentifier(
                identifier = IDENTIFIER_FOLLOWUP,
                content = followupContent,
                trigger = followupTrigger,
            ),
        ) { _: NSError? -> /* result ignored */ }
    }

    actual fun cancel() {
        val ids = listOf(IDENTIFIER_INITIAL, IDENTIFIER_FOLLOWUP)
        center.removePendingNotificationRequestsWithIdentifiers(ids)
        center.removeDeliveredNotificationsWithIdentifiers(ids)
    }
}

private class ForegroundDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        val options: UNNotificationPresentationOptions =
            UNNotificationPresentationOptionBanner or
                UNNotificationPresentationOptionSound or
                UNNotificationPresentationOptionList
        withCompletionHandler(options)
    }
}
