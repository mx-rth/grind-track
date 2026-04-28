package intellij.kmm.settings.grind_track.core.notifications

import intellij.kmm.settings.grind_track.core.database.entity.Routine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.DayOfWeek
import platform.Foundation.NSDateComponents
import platform.Foundation.NSError
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

private const val IDENTIFIER_PREFIX = "routine_"

actual class RoutineNotificationScheduler {

    private val center: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    @OptIn(ExperimentalForeignApi::class)
    actual fun syncAll(routines: List<Routine>) {
        // Cancel all previously-scheduled routine reminders by identifier prefix.
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            val toRemove = requests
                ?.mapNotNull { (it as? UNNotificationRequest)?.identifier }
                ?.filter { it.startsWith(IDENTIFIER_PREFIX) }
                .orEmpty()
            if (toRemove.isNotEmpty()) {
                center.removePendingNotificationRequestsWithIdentifiers(toRemove)
            }
            for (routine in routines) {
                val minute = routine.notificationMinuteOfDay
                if (!routine.notificationEnabled || minute == null || routine.scheduledDays.isEmpty()) {
                    continue
                }
                for (day in routine.scheduledDays) {
                    scheduleOne(routine, day, minute)
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun scheduleOne(routine: Routine, day: DayOfWeek, minuteOfDay: Int) {
        val components = NSDateComponents().apply {
            setWeekday(day.toNSCalendarWeekday().toLong())
            setHour((minuteOfDay / 60).toLong())
            setMinute((minuteOfDay % 60).toLong())
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = components,
            repeats = true,
        )
        val content = UNMutableNotificationContent().apply {
            setTitle(routine.name.ifBlank { "Workout reminder" })
            setBody("Time to work out")
            setSound(UNNotificationSound.defaultSound)
        }
        val identifier = "$IDENTIFIER_PREFIX${routine.id}_day_${day.ordinal}"
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = identifier,
            content = content,
            trigger = trigger,
        )
        center.addNotificationRequest(request) { _: NSError? -> /* result ignored */ }
    }
}

/** kotlinx-datetime: Monday=1..Sunday=7. NSCalendar: Sunday=1..Saturday=7. */
private fun DayOfWeek.toNSCalendarWeekday(): Int = when (this) {
    DayOfWeek.SUNDAY -> 1
    DayOfWeek.MONDAY -> 2
    DayOfWeek.TUESDAY -> 3
    DayOfWeek.WEDNESDAY -> 4
    DayOfWeek.THURSDAY -> 5
    DayOfWeek.FRIDAY -> 6
    DayOfWeek.SATURDAY -> 7
}
