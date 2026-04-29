package intellij.kmm.settings.grind_track.core.notifications

/**
 * OS-level alarm fired when a workout rest interval ends.
 *
 * Implementations schedule a system alarm so the sound also plays when the app is
 * backgrounded or the screen is off. The in-app countdown in the workout screen is
 * separate and remains the source of truth for the foreground UI.
 */
expect class RestTimerAlarm {
    /**
     * Schedule the alarm to fire after [seconds]. Replaces any previously scheduled
     * alarm for this app.
     */
    fun schedule(seconds: Int, exerciseName: String)

    /**
     * Schedule a single non-repeating notification after [seconds] using the system's
     * default notification sound. Used by features (e.g. distance-exercise countdown)
     * that want a gentle one-shot reminder regardless of the user's custom rest-timer
     * sound choices.
     */
    fun scheduleNotificationOnly(seconds: Int, exerciseName: String)

    /** Cancel any pending alarm. Idempotent. */
    fun cancel()
}
