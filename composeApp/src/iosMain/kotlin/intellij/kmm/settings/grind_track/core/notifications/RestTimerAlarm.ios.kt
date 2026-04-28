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

private const val IDENTIFIER = "rest_timer_alarm"

actual class RestTimerAlarm(
    private val customSoundManager: CustomSoundManager,
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
        val customFilename = customSoundManager.current()?.internalFilename
        val sound = if (customFilename != null) {
            UNNotificationSound.soundNamed(customFilename)
        } else {
            UNNotificationSound.defaultSound
        }
        val content = UNMutableNotificationContent().apply {
            setTitle("Rest complete")
            setBody(if (exerciseName.isNotBlank()) "Time for $exerciseName" else "Time for the next set")
            setSound(sound)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = seconds.toDouble().coerceAtLeast(1.0),
            repeats = false,
        )
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = IDENTIFIER,
            content = content,
            trigger = trigger,
        )
        center.addNotificationRequest(request) { _: NSError? -> /* result ignored */ }
    }

    actual fun cancel() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(IDENTIFIER))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(IDENTIFIER))
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
