package intellij.kmm.settings.grind_track.core.data

import intellij.kmm.settings.grind_track.core.database.dao.ExerciseDao
import intellij.kmm.settings.grind_track.core.database.dao.RoutineDao
import intellij.kmm.settings.grind_track.core.database.dao.RoutineExerciseDao
import intellij.kmm.settings.grind_track.core.database.dao.SetEntryDao
import intellij.kmm.settings.grind_track.core.database.dao.WorkoutSessionDao
import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import intellij.kmm.settings.grind_track.core.database.entity.RoutineExercise
import intellij.kmm.settings.grind_track.core.database.entity.SetEntry
import intellij.kmm.settings.grind_track.core.database.entity.WorkoutSession
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

class SeedDataManager(
    private val exerciseDao: ExerciseDao,
    private val routineDao: RoutineDao,
    private val routineExerciseDao: RoutineExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val setEntryDao: SetEntryDao,
) {
    suspend fun seedIfEmpty() {
        if (exerciseDao.observeAll().first().isNotEmpty()) return

        // ── Exercises ──────────────────────────────────────────────────────────
        val benchId    = exerciseDao.insert(Exercise(name = "Bench Press",     defaultRestSeconds = 180))
        val ohpId      = exerciseDao.insert(Exercise(name = "Overhead Press",  defaultRestSeconds = 120))
        val tricepId   = exerciseDao.insert(Exercise(name = "Tricep Pushdown", defaultRestSeconds = 90))
        val deadliftId = exerciseDao.insert(Exercise(name = "Deadlift",        defaultRestSeconds = 240))
        val rowId      = exerciseDao.insert(Exercise(name = "Barbell Row",     defaultRestSeconds = 150))
        val curlId     = exerciseDao.insert(Exercise(name = "Bicep Curl",      defaultRestSeconds = 90))

        // ── Routines ───────────────────────────────────────────────────────────
        val epoch = Instant.parse("2026-03-31T08:00:00Z")
        val pushId = routineDao.insert(Routine(name = "Push Day", createdAt = epoch))
        val pullId = routineDao.insert(Routine(name = "Pull Day", createdAt = epoch + 1.minutes))

        // ── Routine exercises ──────────────────────────────────────────────────
        val reBench    = routineExerciseDao.insert(RoutineExercise(routineId = pushId, exerciseId = benchId,    position = 0, targetSets = 4, targetReps = 8))
        val reOhp      = routineExerciseDao.insert(RoutineExercise(routineId = pushId, exerciseId = ohpId,      position = 1, targetSets = 3, targetReps = 10))
        val reTricep   = routineExerciseDao.insert(RoutineExercise(routineId = pushId, exerciseId = tricepId,   position = 2, targetSets = 3, targetReps = 12))
        val reDeadlift = routineExerciseDao.insert(RoutineExercise(routineId = pullId, exerciseId = deadliftId, position = 0, targetSets = 3, targetReps = 5))
        val reRow      = routineExerciseDao.insert(RoutineExercise(routineId = pullId, exerciseId = rowId,      position = 1, targetSets = 4, targetReps = 8))
        val reCurl     = routineExerciseDao.insert(RoutineExercise(routineId = pullId, exerciseId = curlId,     position = 2, targetSets = 3, targetReps = 12))

        // ── Session helper ─────────────────────────────────────────────────────
        // Each entry in [sets] is (routineExerciseId, weight, reps).
        // Sets are spaced 8 minutes apart within the session.
        suspend fun session(routineId: Long, start: Instant, sets: List<Triple<Long, Double, Int>>) {
            val sessionId = workoutSessionDao.insert(
                WorkoutSession(routineId = routineId, startedAt = start, finishedAt = start + 90.minutes)
            )
            sets.forEachIndexed { idx, (reId, weight, reps) ->
                setEntryDao.insert(
                    SetEntry(
                        sessionId = sessionId,
                        routineExerciseId = reId,
                        setIndex = idx,
                        weight = weight,
                        reps = reps,
                        completedAt = start + (idx * 8 + 2).minutes,
                    )
                )
            }
        }

        // ── Push sessions (5 sessions) ─────────────────────────────────────────
        // Apr 7
        session(pushId, Instant.parse("2026-04-07T09:00:00Z"), listOf(
            Triple(reBench,  80.0, 8), Triple(reBench,  80.0, 8), Triple(reBench,  77.5, 8), Triple(reBench,  75.0, 7),
            Triple(reOhp,    50.0, 10), Triple(reOhp,   50.0, 10), Triple(reOhp,   47.5, 9),
            Triple(reTricep, 30.0, 12), Triple(reTricep, 30.0, 12), Triple(reTricep, 27.5, 12),
        ))
        // Apr 11
        session(pushId, Instant.parse("2026-04-11T09:00:00Z"), listOf(
            Triple(reBench,  82.5, 8), Triple(reBench,  82.5, 8), Triple(reBench,  80.0, 8), Triple(reBench,  77.5, 8),
            Triple(reOhp,    52.5, 10), Triple(reOhp,   52.5, 10), Triple(reOhp,   50.0, 9),
            Triple(reTricep, 32.5, 12), Triple(reTricep, 32.5, 12), Triple(reTricep, 30.0, 12),
        ))
        // Apr 16
        session(pushId, Instant.parse("2026-04-16T09:00:00Z"), listOf(
            Triple(reBench,  85.0, 8), Triple(reBench,  85.0, 8), Triple(reBench,  82.5, 8), Triple(reBench,  80.0, 8),
            Triple(reOhp,    55.0, 10), Triple(reOhp,   55.0, 10), Triple(reOhp,   52.5, 10),
            Triple(reTricep, 35.0, 12), Triple(reTricep, 35.0, 12), Triple(reTricep, 32.5, 11),
        ))
        // Apr 21
        session(pushId, Instant.parse("2026-04-21T09:00:00Z"), listOf(
            Triple(reBench,  87.5, 8), Triple(reBench,  87.5, 8), Triple(reBench,  85.0, 8), Triple(reBench,  82.5, 8),
            Triple(reOhp,    57.5, 10), Triple(reOhp,   57.5, 10), Triple(reOhp,   55.0, 10),
            Triple(reTricep, 37.5, 12), Triple(reTricep, 37.5, 12), Triple(reTricep, 35.0, 12),
        ))
        // Apr 25
        session(pushId, Instant.parse("2026-04-25T09:00:00Z"), listOf(
            Triple(reBench,  90.0, 8), Triple(reBench,  90.0, 8), Triple(reBench,  87.5, 8), Triple(reBench,  85.0, 7),
            Triple(reOhp,    60.0, 10), Triple(reOhp,   60.0, 9), Triple(reOhp,   57.5, 9),
            Triple(reTricep, 40.0, 12), Triple(reTricep, 40.0, 12), Triple(reTricep, 37.5, 12),
        ))

        // ── Pull sessions (4 sessions) ─────────────────────────────────────────
        // Apr 9
        session(pullId, Instant.parse("2026-04-09T09:00:00Z"), listOf(
            Triple(reDeadlift, 100.0, 5), Triple(reDeadlift, 100.0, 5), Triple(reDeadlift,  97.5, 5),
            Triple(reRow,  60.0, 8), Triple(reRow,  60.0, 8), Triple(reRow,  57.5, 8), Triple(reRow,  55.0, 8),
            Triple(reCurl, 15.0, 12), Triple(reCurl, 15.0, 12), Triple(reCurl, 12.5, 12),
        ))
        // Apr 14
        session(pullId, Instant.parse("2026-04-14T09:00:00Z"), listOf(
            Triple(reDeadlift, 105.0, 5), Triple(reDeadlift, 105.0, 5), Triple(reDeadlift, 102.5, 5),
            Triple(reRow,  62.5, 8), Triple(reRow,  62.5, 8), Triple(reRow,  60.0, 8), Triple(reRow,  57.5, 7),
            Triple(reCurl, 17.5, 12), Triple(reCurl, 17.5, 12), Triple(reCurl, 15.0, 12),
        ))
        // Apr 18
        session(pullId, Instant.parse("2026-04-18T09:00:00Z"), listOf(
            Triple(reDeadlift, 110.0, 5), Triple(reDeadlift, 110.0, 5), Triple(reDeadlift, 107.5, 5),
            Triple(reRow,  65.0, 8), Triple(reRow,  65.0, 8), Triple(reRow,  62.5, 8), Triple(reRow,  60.0, 8),
            Triple(reCurl, 17.5, 12), Triple(reCurl, 17.5, 12), Triple(reCurl, 15.0, 11),
        ))
        // Apr 23
        session(pullId, Instant.parse("2026-04-23T09:00:00Z"), listOf(
            Triple(reDeadlift, 115.0, 5), Triple(reDeadlift, 115.0, 5), Triple(reDeadlift, 112.5, 5),
            Triple(reRow,  67.5, 8), Triple(reRow,  67.5, 8), Triple(reRow,  65.0, 8), Triple(reRow,  62.5, 8),
            Triple(reCurl, 20.0, 12), Triple(reCurl, 20.0, 12), Triple(reCurl, 17.5, 12),
        ))
    }
}
