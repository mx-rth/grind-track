package intellij.kmm.settings.grind_track.core.notifications

import intellij.kmm.settings.grind_track.core.database.entity.Routine

expect class RoutineNotificationScheduler {
    /**
     * Cancel any previously-scheduled routine reminders and schedule fresh ones for every
     * routine where notifications are enabled, a time has been picked, and at least one
     * weekday is scheduled. Idempotent.
     */
    fun syncAll(routines: List<Routine>)
}
