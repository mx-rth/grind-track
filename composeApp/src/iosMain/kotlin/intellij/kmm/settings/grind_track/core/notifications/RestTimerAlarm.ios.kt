package intellij.kmm.settings.grind_track.core.notifications

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
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

/**
 * iOS lacks a true "play continuous alarm" notification API outside the Critical Alerts
 * entitlement (which requires Apple approval). To approximate a long alarm tone with
 * standard APIs, we schedule a sequence of notifications spaced [REPEAT_INTERVAL_SECONDS]
 * seconds apart so the device chimes repeatedly for [REPEAT_COUNT * REPEAT_INTERVAL_SECONDS]
 * seconds. They share a [THREAD_IDENTIFIER] so iOS groups them visually on the lock screen.
 */
private const val IDENTIFIER_PREFIX = "rest_timer_alarm_"
private const val THREAD_IDENTIFIER = "rest_timer_alarm"
private const val REPEAT_COUNT = 6
private const val REPEAT_INTERVAL_SECONDS = 4.0

actual class RestTimerAlarm {

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
        val firstFireSeconds = seconds.toDouble().coerceAtLeast(1.0)
        val title = "Rest complete"
        val body = if (exerciseName.isNotBlank()) "Time for $exerciseName" else "Time for the next set"

        for (i in 0 until REPEAT_COUNT) {
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                if (i == 0) setBody(body) else setBody(" ")
                setSound(UNNotificationSound.defaultSound)
                setThreadIdentifier(THREAD_IDENTIFIER)
            }
            val triggerSeconds = firstFireSeconds + i * REPEAT_INTERVAL_SECONDS
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                timeInterval = triggerSeconds,
                repeats = false,
            )
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = IDENTIFIER_PREFIX + i,
                content = content,
                trigger = trigger,
            )
            center.addNotificationRequest(request) { _: NSError? -> /* result ignored */ }
        }
    }

    actual fun cancel() {
        val ids = (0 until REPEAT_COUNT).map { IDENTIFIER_PREFIX + it }
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
