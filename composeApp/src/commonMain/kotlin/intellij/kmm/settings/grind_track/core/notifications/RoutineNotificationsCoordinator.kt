package intellij.kmm.settings.grind_track.core.notifications

import intellij.kmm.settings.grind_track.core.data.RoutineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Observes the routine list and re-syncs the platform [RoutineNotificationScheduler] on
 * every change. This makes notifications reactive to renames, schedule changes, time
 * changes, toggles, additions, and deletions without scattering scheduling calls across
 * mutation paths.
 */
class RoutineNotificationsCoordinator(
    private val repository: RoutineRepository,
    private val scheduler: RoutineNotificationScheduler,
    scope: CoroutineScope,
) {
    init {
        scope.launch {
            repository.observeRoutines().collectLatest { routines ->
                scheduler.syncAll(routines)
            }
        }
    }
}
