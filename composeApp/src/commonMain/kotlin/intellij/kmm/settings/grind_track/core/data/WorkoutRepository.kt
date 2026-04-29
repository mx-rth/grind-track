package intellij.kmm.settings.grind_track.core.data

import intellij.kmm.settings.grind_track.core.database.dao.SetEntryDao
import intellij.kmm.settings.grind_track.core.database.dao.WorkoutSessionDao
import intellij.kmm.settings.grind_track.core.database.entity.SetEntry
import intellij.kmm.settings.grind_track.core.database.entity.Side
import intellij.kmm.settings.grind_track.core.database.entity.WorkoutSession
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val sessionDao: WorkoutSessionDao,
    private val setEntryDao: SetEntryDao,
) {
    fun observeActiveSession(): Flow<WorkoutSession?> = sessionDao.observeActive()

    suspend fun startSession(routineId: Long): Long =
        sessionDao.insert(
            WorkoutSession(routineId = routineId, startedAt = Clock.System.now())
        )

    suspend fun finishSession(sessionId: Long) {
        val session = sessionDao.getById(sessionId) ?: return
        if (session.finishedAt != null) return
        sessionDao.update(session.copy(finishedAt = Clock.System.now()))
    }

    suspend fun recordSet(
        sessionId: Long,
        routineExerciseId: Long,
        setIndex: Int,
        weight: Double = 0.0,
        reps: Int = 0,
        side: Side? = null,
        distanceMeters: Int? = null,
        durationSeconds: Double? = null,
    ): Long = setEntryDao.insert(
        SetEntry(
            sessionId = sessionId,
            routineExerciseId = routineExerciseId,
            setIndex = setIndex,
            weight = weight,
            reps = reps,
            completedAt = Clock.System.now(),
            side = side,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
        )
    )
}
