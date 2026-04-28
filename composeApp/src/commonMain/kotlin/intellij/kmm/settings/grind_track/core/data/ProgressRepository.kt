package intellij.kmm.settings.grind_track.core.data

import intellij.kmm.settings.grind_track.core.database.dao.RoutineDao
import intellij.kmm.settings.grind_track.core.database.dao.SetEntryDao
import intellij.kmm.settings.grind_track.core.database.dao.WorkoutSessionDao
import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import intellij.kmm.settings.grind_track.core.database.entity.SetEntry
import intellij.kmm.settings.grind_track.core.database.entity.WorkoutSession
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class ProgressRepository(
    private val setEntryDao: SetEntryDao,
    private val routineDao: RoutineDao,
    private val workoutSessionDao: WorkoutSessionDao,
) {
    fun observeExercisesWithActivity(): Flow<List<Exercise>> =
        setEntryDao.observeExercisesWithActivity()

    fun observeHistoryForExercise(exerciseId: Long): Flow<List<SetEntry>> =
        setEntryDao.observeHistoryForExercise(exerciseId)

    fun observeStreak(): Flow<Int> = combine(
        routineDao.observeAll(),
        workoutSessionDao.observeAll(),
    ) { routines, sessions ->
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        computeGlobalStreak(routines, sessions, today, tz)
    }

    private fun computeGlobalStreak(
        routines: List<Routine>,
        sessions: List<WorkoutSession>,
        today: LocalDate,
        tz: TimeZone,
    ): Int {
        if (routines.none { it.scheduledDays.isNotEmpty() }) return 0

        // Map routineId → set of dates where the session was completed
        val completedDatesByRoutine: Map<Long, Set<LocalDate>> = sessions
            .filter { it.finishedAt != null && it.routineId != null }
            .groupBy { it.routineId!! }
            .mapValues { (_, s) -> s.mapTo(mutableSetOf()) { it.startedAt.toLocalDateTime(tz).date } }

        var streak = 0
        var checkDate = today
        val limit = today.minus(365, DateTimeUnit.DAY)
        var todaySkipped = false

        while (checkDate >= limit) {
            val scheduledToday = routines.filter { checkDate.dayOfWeek in it.scheduledDays }

            if (scheduledToday.isEmpty()) {
                // No workout scheduled → pause streak, don't break it
                checkDate = checkDate.minus(1, DateTimeUnit.DAY)
                continue
            }

            val anyCompleted = scheduledToday.any { routine ->
                completedDatesByRoutine[routine.id]?.contains(checkDate) == true
            }

            if (checkDate == today && !anyCompleted && !todaySkipped) {
                // Today is a workout day but hasn't been done yet — still time, skip without penalty
                todaySkipped = true
                checkDate = checkDate.minus(1, DateTimeUnit.DAY)
                continue
            }

            if (anyCompleted) {
                streak++
            } else {
                break
            }

            checkDate = checkDate.minus(1, DateTimeUnit.DAY)
        }

        return streak
    }
}
