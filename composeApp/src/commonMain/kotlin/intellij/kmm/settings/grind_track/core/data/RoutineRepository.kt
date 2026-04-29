package intellij.kmm.settings.grind_track.core.data

import intellij.kmm.settings.grind_track.core.database.dao.ExerciseDao
import intellij.kmm.settings.grind_track.core.database.dao.RoutineDao
import intellij.kmm.settings.grind_track.core.database.dao.RoutineExerciseDao
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import intellij.kmm.settings.grind_track.core.database.entity.RoutineExercise
import kotlin.time.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val routineExerciseDao: RoutineExerciseDao,
    private val exerciseDao: ExerciseDao,
) {
    fun observeRoutines(): Flow<List<Routine>> = routineDao.observeAll()

    fun observeRoutine(id: Long): Flow<Routine?> = routineDao.observeById(id)

    fun observeExercisesForRoutine(routineId: Long): Flow<List<RoutineExerciseWithExercise>> =
        combine(
            routineExerciseDao.observeForRoutine(routineId),
            exerciseDao.observeAll(),
        ) { routineExercises, exercises ->
            val byId = exercises.associateBy { it.id }
            routineExercises.mapNotNull { re ->
                byId[re.exerciseId]?.let { RoutineExerciseWithExercise(re, it) }
            }
        }

    suspend fun createRoutine(name: String): Long =
        routineDao.insert(Routine(name = name.trim(), createdAt = Clock.System.now()))

    suspend fun renameRoutine(routine: Routine, name: String) {
        routineDao.update(routine.copy(name = name.trim()))
    }

    suspend fun setScheduledDays(routine: Routine, days: Set<DayOfWeek>) {
        routineDao.update(routine.copy(scheduledDays = days))
    }

    suspend fun setNotificationEnabled(routine: Routine, enabled: Boolean) {
        routineDao.update(routine.copy(notificationEnabled = enabled))
    }

    suspend fun setNotificationMinuteOfDay(routine: Routine, minute: Int?) {
        routineDao.update(routine.copy(notificationMinuteOfDay = minute))
    }

    suspend fun deleteRoutine(id: Long) = routineDao.deleteById(id)

    suspend fun addExerciseToRoutine(
        routineId: Long,
        exerciseId: Long,
        targetSets: Int,
        targetReps: Int?,
        restSecondsOverride: Int? = null,
        restBetweenExercisesOverride: Int? = null,
        restAfterFirstSideSecondsOverride: Int? = null,
        startingSide: intellij.kmm.settings.grind_track.core.database.entity.Side =
            intellij.kmm.settings.grind_track.core.database.entity.Side.LEFT,
        targetDistanceMeters: Int? = null,
        targetDurationSeconds: Double? = null,
    ): Long {
        val nextPosition = routineExerciseDao.maxPosition(routineId) + 1
        return routineExerciseDao.insert(
            RoutineExercise(
                routineId = routineId,
                exerciseId = exerciseId,
                position = nextPosition,
                targetSets = targetSets,
                targetReps = targetReps,
                restSecondsOverride = restSecondsOverride,
                restBetweenExercisesOverride = restBetweenExercisesOverride,
                restAfterFirstSideSecondsOverride = restAfterFirstSideSecondsOverride,
                startingSide = startingSide,
                targetDistanceMeters = targetDistanceMeters,
                targetDurationSeconds = targetDurationSeconds,
            )
        )
    }

    suspend fun updateRoutineExercise(routineExercise: RoutineExercise) =
        routineExerciseDao.update(routineExercise)

    suspend fun removeRoutineExercise(id: Long) = routineExerciseDao.deleteById(id)

    suspend fun moveUp(routineExerciseId: Long) = swapWithNeighbour(routineExerciseId, -1)

    suspend fun moveDown(routineExerciseId: Long) = swapWithNeighbour(routineExerciseId, +1)

    private suspend fun swapWithNeighbour(routineExerciseId: Long, delta: Int) {
        val current = routineExerciseDao.getById(routineExerciseId) ?: return
        val siblings = routineExerciseDao.listForRoutine(current.routineId)
        val idx = siblings.indexOfFirst { it.id == current.id }
        val targetIdx = idx + delta
        if (idx == -1 || targetIdx !in siblings.indices) return
        routineExerciseDao.swapPositions(siblings[idx], siblings[targetIdx])
    }
}
