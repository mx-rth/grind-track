package intellij.kmm.settings.grind_track.core.data

import intellij.kmm.settings.grind_track.core.database.dao.ExerciseDao
import intellij.kmm.settings.grind_track.core.database.dao.RoutineDao
import intellij.kmm.settings.grind_track.core.database.dao.RoutineExerciseDao
import intellij.kmm.settings.grind_track.core.database.dao.SetEntryDao
import intellij.kmm.settings.grind_track.core.database.dao.WorkoutSessionDao
import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import intellij.kmm.settings.grind_track.core.database.entity.ExerciseType
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import intellij.kmm.settings.grind_track.core.database.entity.RoutineExercise
import intellij.kmm.settings.grind_track.core.database.entity.SetEntry
import intellij.kmm.settings.grind_track.core.database.entity.Side
import intellij.kmm.settings.grind_track.core.database.entity.WorkoutSession
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant
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
        val treadmillId = exerciseDao.insert(
            Exercise(name = "Treadmill Run", defaultRestSeconds = 0, type = ExerciseType.DISTANCE)
        )
        val plankId = exerciseDao.insert(
            Exercise(name = "Plank Hold", defaultRestSeconds = 60, type = ExerciseType.TIME)
        )
        val ropeId = exerciseDao.insert(
            Exercise(name = "Jump Rope", defaultRestSeconds = 60, type = ExerciseType.TIME)
        )
        val oneArmPushupId = exerciseDao.insert(
            Exercise(
                name = "One-Arm Pushup",
                defaultRestSeconds = 90,
                unilateral = true,
                bodyWeight = true,
            )
        )
        val dipsId = exerciseDao.insert(
            Exercise(name = "Dips", defaultRestSeconds = 120, bodyWeight = true)
        )
        val benchId = exerciseDao.insert(
            Exercise(name = "Bench Press", defaultRestSeconds = 180)
        )
        val rowId = exerciseDao.insert(
            Exercise(name = "Barbell Row", defaultRestSeconds = 150)
        )
        val squatId = exerciseDao.insert(
            Exercise(name = "Back Squat", defaultRestSeconds = 180)
        )
        val rdlId = exerciseDao.insert(
            Exercise(name = "Romanian Deadlift", defaultRestSeconds = 150)
        )
        val lungeId = exerciseDao.insert(
            Exercise(name = "Walking Lunge", defaultRestSeconds = 90, bodyWeight = true)
        )
        val lateralRaiseId = exerciseDao.insert(
            Exercise(name = "Lateral Raise", defaultRestSeconds = 75)
        )

        // ── Routines ───────────────────────────────────────────────────────────
        val epoch = Instant.parse("2026-03-15T08:00:00Z")
        val cardioId = routineDao.insert(
            Routine(
                name = "Cardio",
                createdAt = epoch,
                scheduledDays = setOf(DayOfWeek.MONDAY),
            )
        )
        val pushPullId = routineDao.insert(
            Routine(
                name = "Push + Pull",
                createdAt = epoch + 1.minutes,
                scheduledDays = setOf(DayOfWeek.WEDNESDAY),
            )
        )
        val legsId = routineDao.insert(
            Routine(
                name = "Legs",
                createdAt = epoch + 2.minutes,
                scheduledDays = setOf(DayOfWeek.FRIDAY),
            )
        )
        val shouldersId = routineDao.insert(
            Routine(
                name = "Shoulders",
                createdAt = epoch + 3.minutes,
                scheduledDays = setOf(DayOfWeek.THURSDAY),
            )
        )

        // ── Routine exercises ──────────────────────────────────────────────────
        // Cardio
        val reTreadmill = routineExerciseDao.insert(
            RoutineExercise(
                routineId = cardioId, exerciseId = treadmillId, position = 0,
                targetSets = 1, targetReps = null, targetDistanceMeters = 5000,
            )
        )
        val rePlank = routineExerciseDao.insert(
            RoutineExercise(
                routineId = cardioId, exerciseId = plankId, position = 1,
                targetSets = 3, targetReps = null, targetDurationSeconds = 60.0,
            )
        )
        val reRope = routineExerciseDao.insert(
            RoutineExercise(
                routineId = cardioId, exerciseId = ropeId, position = 2,
                targetSets = 3, targetReps = null, targetDurationSeconds = 120.0,
            )
        )

        // Push + Pull
        val reOneArm = routineExerciseDao.insert(
            RoutineExercise(
                routineId = pushPullId, exerciseId = oneArmPushupId, position = 0,
                targetSets = 3, targetReps = 8,
            )
        )
        val reDips = routineExerciseDao.insert(
            RoutineExercise(
                routineId = pushPullId, exerciseId = dipsId, position = 1,
                targetSets = 4, targetReps = 10,
            )
        )
        val reBench = routineExerciseDao.insert(
            RoutineExercise(
                routineId = pushPullId, exerciseId = benchId, position = 2,
                targetSets = 4, targetReps = 8,
            )
        )
        val reRow = routineExerciseDao.insert(
            RoutineExercise(
                routineId = pushPullId, exerciseId = rowId, position = 3,
                targetSets = 4, targetReps = 8,
            )
        )

        // Legs
        val reSquat = routineExerciseDao.insert(
            RoutineExercise(
                routineId = legsId, exerciseId = squatId, position = 0,
                targetSets = 4, targetReps = 8,
            )
        )
        val reRdl = routineExerciseDao.insert(
            RoutineExercise(
                routineId = legsId, exerciseId = rdlId, position = 1,
                targetSets = 3, targetReps = 10,
            )
        )
        val reLunge = routineExerciseDao.insert(
            RoutineExercise(
                routineId = legsId, exerciseId = lungeId, position = 2,
                targetSets = 3, targetReps = 12,
            )
        )

        // Shoulders — lateral raises 5 sets to failure (targetReps = null).
        val reLateral = routineExerciseDao.insert(
            RoutineExercise(
                routineId = shouldersId, exerciseId = lateralRaiseId, position = 0,
                targetSets = 5, targetReps = null,
            )
        )

        // ── Builders ───────────────────────────────────────────────────────────
        suspend fun session(
            routineId: Long,
            start: Instant,
            durationMinutes: Int,
            sets: List<SetEntry>,
        ) {
            val sessionId = workoutSessionDao.insert(
                WorkoutSession(
                    routineId = routineId,
                    startedAt = start,
                    finishedAt = start + durationMinutes.minutes,
                )
            )
            sets.forEach { setEntryDao.insert(it.copy(sessionId = sessionId)) }
        }

        fun strengthSets(reId: Long, weights: List<Double>, reps: Int, start: Instant): List<SetEntry> =
            weights.mapIndexed { i, w ->
                SetEntry(
                    sessionId = 0L,
                    routineExerciseId = reId,
                    setIndex = i + 1,
                    weight = w,
                    reps = reps,
                    completedAt = start + (i * 3 + 2).minutes,
                )
            }

        fun bodyweightSets(reId: Long, repsList: List<Int>, start: Instant): List<SetEntry> =
            repsList.mapIndexed { i, r ->
                SetEntry(
                    sessionId = 0L,
                    routineExerciseId = reId,
                    setIndex = i + 1,
                    weight = 0.0,
                    reps = r,
                    completedAt = start + (i * 3 + 2).minutes,
                )
            }

        // repsPerSide is List<(leftReps, rightReps)> — one pair per set.
        fun unilateralSets(
            reId: Long,
            repsPerSide: List<Pair<Int, Int>>,
            start: Instant,
            startingSide: Side = Side.LEFT,
        ): List<SetEntry> = repsPerSide.flatMapIndexed { i, (leftReps, rightReps) ->
            val firstReps = if (startingSide == Side.LEFT) leftReps else rightReps
            val secondReps = if (startingSide == Side.LEFT) rightReps else leftReps
            listOf(
                SetEntry(
                    sessionId = 0L,
                    routineExerciseId = reId,
                    setIndex = i + 1,
                    weight = 0.0,
                    reps = firstReps,
                    completedAt = start + (i * 4 + 2).minutes,
                    side = startingSide,
                ),
                SetEntry(
                    sessionId = 0L,
                    routineExerciseId = reId,
                    setIndex = i + 1,
                    weight = 0.0,
                    reps = secondReps,
                    completedAt = start + (i * 4 + 3).minutes,
                    side = startingSide.other(),
                ),
            )
        }

        fun distanceSets(reId: Long, distances: List<Int>, start: Instant): List<SetEntry> =
            distances.mapIndexed { i, d ->
                SetEntry(
                    sessionId = 0L,
                    routineExerciseId = reId,
                    setIndex = i + 1,
                    weight = 0.0,
                    reps = 0,
                    completedAt = start + (i * 5 + 2).minutes,
                    distanceMeters = d,
                )
            }

        fun timeSets(reId: Long, durations: List<Double>, start: Instant): List<SetEntry> =
            durations.mapIndexed { i, d ->
                SetEntry(
                    sessionId = 0L,
                    routineExerciseId = reId,
                    setIndex = i + 1,
                    weight = 0.0,
                    reps = 0,
                    completedAt = start + (i * 3 + 2).minutes,
                    durationSeconds = d,
                )
            }

        // ── Cardio sessions (Mondays, last 6 weeks) ────────────────────────────
        val cardioDays = listOf(
            "2026-03-23", "2026-03-30", "2026-04-06", "2026-04-13", "2026-04-20", "2026-04-27",
        )
        val treadmillDistances = listOf(4500, 4700, 4800, 5000, 5100, 5300)
        val plankDurations = listOf(
            listOf(45.0, 40.0, 35.0),
            listOf(50.0, 45.0, 40.0),
            listOf(55.0, 50.0, 45.0),
            listOf(60.0, 55.0, 50.0),
            listOf(65.0, 60.0, 55.0),
            listOf(70.0, 65.0, 60.0),
        )
        val ropeDurations = listOf(
            listOf(90.0, 80.0, 75.0),
            listOf(100.0, 90.0, 85.0),
            listOf(110.0, 100.0, 90.0),
            listOf(120.0, 110.0, 100.0),
            listOf(130.0, 120.0, 110.0),
            listOf(140.0, 130.0, 120.0),
        )
        cardioDays.forEachIndexed { week, day ->
            val start = Instant.parse("${day}T07:30:00Z")
            session(
                routineId = cardioId,
                start = start,
                durationMinutes = 45,
                sets = buildList {
                    addAll(distanceSets(reTreadmill, listOf(treadmillDistances[week]), start))
                    addAll(timeSets(rePlank, plankDurations[week], start + 25.minutes))
                    addAll(timeSets(reRope, ropeDurations[week], start + 35.minutes))
                },
            )
        }

        // ── Push + Pull sessions (Wednesdays, last 6 weeks) ────────────────────
        val pushPullDays = listOf(
            "2026-03-25", "2026-04-01", "2026-04-08", "2026-04-15", "2026-04-22", "2026-04-29",
        )
        // (left, right) reps per set — slight asymmetry, dominant left arm starts.
        val oneArmReps = listOf(
            listOf(6 to 5, 5 to 5, 4 to 4),
            listOf(7 to 6, 6 to 5, 5 to 5),
            listOf(7 to 7, 6 to 6, 5 to 5),
            listOf(8 to 7, 7 to 6, 6 to 6),
            listOf(8 to 8, 7 to 7, 6 to 6),
            listOf(9 to 8, 8 to 7, 7 to 7),
        )
        val dipsReps = listOf(
            listOf(8, 8, 7, 6),
            listOf(9, 9, 8, 7),
            listOf(10, 10, 8, 7),
            listOf(11, 10, 9, 8),
            listOf(12, 11, 10, 8),
            listOf(13, 12, 10, 9),
        )
        val benchWeights = listOf(
            listOf(70.0, 70.0, 67.5, 65.0),
            listOf(72.5, 72.5, 70.0, 67.5),
            listOf(75.0, 75.0, 72.5, 70.0),
            listOf(77.5, 77.5, 75.0, 72.5),
            listOf(80.0, 80.0, 77.5, 75.0),
            listOf(82.5, 82.5, 80.0, 77.5),
        )
        val rowWeights = listOf(
            listOf(55.0, 55.0, 52.5, 50.0),
            listOf(57.5, 57.5, 55.0, 52.5),
            listOf(60.0, 60.0, 57.5, 55.0),
            listOf(62.5, 62.5, 60.0, 57.5),
            listOf(65.0, 65.0, 62.5, 60.0),
            listOf(67.5, 67.5, 65.0, 62.5),
        )
        pushPullDays.forEachIndexed { week, day ->
            val start = Instant.parse("${day}T18:00:00Z")
            session(
                routineId = pushPullId,
                start = start,
                durationMinutes = 75,
                sets = buildList {
                    addAll(unilateralSets(reOneArm, oneArmReps[week], start))
                    addAll(bodyweightSets(reDips, dipsReps[week], start + 18.minutes))
                    addAll(strengthSets(reBench, benchWeights[week], 8, start + 35.minutes))
                    addAll(strengthSets(reRow, rowWeights[week], 8, start + 55.minutes))
                },
            )
        }

        // ── Legs sessions (Fridays, last 6 weeks) ──────────────────────────────
        val legsDays = listOf(
            "2026-03-20", "2026-03-27", "2026-04-03", "2026-04-10", "2026-04-17", "2026-04-24",
        )
        val squatWeights = listOf(
            listOf(80.0, 80.0, 77.5, 75.0),
            listOf(82.5, 82.5, 80.0, 77.5),
            listOf(85.0, 85.0, 82.5, 80.0),
            listOf(87.5, 87.5, 85.0, 82.5),
            listOf(90.0, 90.0, 87.5, 85.0),
            listOf(92.5, 92.5, 90.0, 87.5),
        )
        val rdlWeights = listOf(
            listOf(60.0, 60.0, 57.5),
            listOf(62.5, 62.5, 60.0),
            listOf(65.0, 65.0, 62.5),
            listOf(67.5, 67.5, 65.0),
            listOf(70.0, 70.0, 67.5),
            listOf(72.5, 72.5, 70.0),
        )
        val lungeReps = listOf(
            listOf(12, 12, 10),
            listOf(12, 12, 11),
            listOf(13, 12, 12),
            listOf(14, 13, 12),
            listOf(14, 14, 12),
            listOf(15, 14, 13),
        )
        legsDays.forEachIndexed { week, day ->
            val start = Instant.parse("${day}T17:30:00Z")
            session(
                routineId = legsId,
                start = start,
                durationMinutes = 65,
                sets = buildList {
                    addAll(strengthSets(reSquat, squatWeights[week], 8, start))
                    addAll(strengthSets(reRdl, rdlWeights[week], 10, start + 25.minutes))
                    addAll(bodyweightSets(reLunge, lungeReps[week], start + 45.minutes))
                },
            )
        }

        // ── Shoulders sessions (Thursdays, last 3 weeks, to failure) ──────────
        val shouldersDays = listOf("2026-04-09", "2026-04-16", "2026-04-23")
        // (weight, repsPerSet) — same weight across the 5 sets, reps drop with fatigue.
        val lateralRaiseLog = listOf(
            8.0 to listOf(15, 12, 10, 9, 7),
            9.0 to listOf(15, 13, 11, 9, 8),
            10.0 to listOf(14, 12, 11, 9, 7),
        )
        shouldersDays.forEachIndexed { week, day ->
            val start = Instant.parse("${day}T18:30:00Z")
            val (weight, reps) = lateralRaiseLog[week]
            session(
                routineId = shouldersId,
                start = start,
                durationMinutes = 25,
                sets = reps.mapIndexed { i, r ->
                    SetEntry(
                        sessionId = 0L,
                        routineExerciseId = reLateral,
                        setIndex = i + 1,
                        weight = weight,
                        reps = r,
                        completedAt = start + (i * 2 + 2).minutes,
                    )
                },
            )
        }
    }
}
