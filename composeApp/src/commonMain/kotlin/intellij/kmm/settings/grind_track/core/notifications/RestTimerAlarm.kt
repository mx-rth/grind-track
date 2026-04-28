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

    /** Cancel any pending alarm. Idempotent. */
    fun cancel()
}
