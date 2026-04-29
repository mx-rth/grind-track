package intellij.kmm.settings.grind_track.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import intellij.kmm.settings.grind_track.core.data.RoutineExerciseWithExercise
import intellij.kmm.settings.grind_track.core.data.RoutineRepository
import intellij.kmm.settings.grind_track.core.data.WorkoutRepository
import intellij.kmm.settings.grind_track.core.notifications.RestTimerAlarm
import intellij.kmm.settings.grind_track.core.database.entity.ExerciseType
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import intellij.kmm.settings.grind_track.core.database.entity.Side
import intellij.kmm.settings.grind_track.core.database.entity.WorkoutSession
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Phase {
    data object Working : Phase

    data class Resting(
        val totalSeconds: Int,
        val remainingSeconds: Int,
        val weightDraft: String = "",
        val repsDraft: String = "",
        val distanceDraft: String = "",
        val durationDraft: String = "",
        val isLogged: Boolean = false,
        val side: Side? = null,
        val isInterSideRest: Boolean = false,
    ) : Phase

    data class RestingBeforeNextExercise(
        val totalSeconds: Int,
        val remainingSeconds: Int,
        val nextExerciseName: String,
    ) : Phase
}

sealed interface WorkoutUiState {
    data object Loading : WorkoutUiState

    data class Picking(val routines: List<Routine>) : WorkoutUiState

    data class InSession(
        val session: WorkoutSession,
        val routine: Routine,
        val exercises: List<RoutineExerciseWithExercise>,
        val currentExerciseIndex: Int,
        val currentSetIndex: Int,
        val currentSideIndex: Int,
        val phase: Phase,
    ) : WorkoutUiState {
        val currentExercise: RoutineExerciseWithExercise?
            get() = exercises.getOrNull(currentExerciseIndex)

        val isLastSetOfExercise: Boolean
            get() = currentSetIndex >= (currentExercise?.routineExercise?.targetSets ?: 0)

        val isLastExercise: Boolean
            get() = currentExerciseIndex >= exercises.lastIndex

        /** Current side, or null when the exercise is not unilateral. */
        val currentSide: Side?
            get() {
                val ex = currentExercise ?: return null
                if (!ex.exercise.unilateral) return null
                val starting = ex.routineExercise.startingSide
                return if (currentSideIndex == 0) starting else starting.other()
            }
    }
}

private data class Position(val exerciseIndex: Int, val setIndex: Int, val sideIndex: Int = 0) {
    companion object {
        val START = Position(0, 1, 0)
    }
}

private data class LastEntry(
    val weight: Double = 0.0,
    val reps: Int = 0,
    val distance: Int = 0,
    val duration: Double = 0.0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val restTimerAlarm: RestTimerAlarm,
) : ViewModel() {

    private val position = MutableStateFlow(Position.START)
    private val phase = MutableStateFlow<Phase>(Phase.Working)

    /** Last submitted measurements per `(routineExerciseId, side?)`, used to prefill the rest form. */
    private val lastSubmitted = mutableMapOf<Pair<Long, Side?>, LastEntry>()

    private var timerJob: Job? = null

    val state: StateFlow<WorkoutUiState> =
        workoutRepository.observeActiveSession()
            .flatMapLatest { session ->
                if (session == null) {
                    routineRepository.observeRoutines()
                        .map { WorkoutUiState.Picking(it) }
                } else {
                    val routineId = session.routineId
                    if (routineId == null) {
                        viewModelScope.launch { workoutRepository.finishSession(session.id) }
                        flowOf(WorkoutUiState.Loading)
                    } else {
                        combine(
                            routineRepository.observeRoutine(routineId),
                            routineRepository.observeExercisesForRoutine(routineId),
                            position,
                            phase,
                        ) { routine, exercises, pos, currentPhase ->
                            if (routine == null) {
                                WorkoutUiState.Loading
                            } else {
                                WorkoutUiState.InSession(
                                    session = session,
                                    routine = routine,
                                    exercises = exercises,
                                    currentExerciseIndex = pos.exerciseIndex,
                                    currentSetIndex = pos.setIndex,
                                    currentSideIndex = pos.sideIndex,
                                    phase = currentPhase,
                                )
                            }
                        }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState.Loading)

    fun startSession(routineId: Long) {
        viewModelScope.launch {
            position.value = Position.START
            phase.value = Phase.Working
            workoutRepository.startSession(routineId)
        }
    }

    /** From Working: enter Resting, prefill the form, start the countdown. */
    fun markSetComplete() {
        val current = state.value as? WorkoutUiState.InSession ?: return
        if (current.phase !is Phase.Working) return
        val exercise = current.currentExercise ?: return
        val unilateral = exercise.exercise.unilateral
        val isFirstSide = unilateral && current.currentSideIndex == 0
        val totalSeconds = if (isFirstSide) {
            exercise.effectiveRestAfterFirstSideSeconds
        } else {
            exercise.effectiveRestSeconds
        }
        val side: Side? = current.currentSide
        val last = lastSubmitted[exercise.routineExercise.id to side] ?: LastEntry()
        val weightDraft = when {
            exercise.exercise.type != ExerciseType.STRENGTH -> ""
            last.weight > 0.0 -> formatWeight(last.weight)
            exercise.exercise.bodyWeight -> "0"
            else -> ""
        }
        val targetReps = exercise.routineExercise.targetReps ?: 0
        val repsDraft = when {
            exercise.exercise.type != ExerciseType.STRENGTH -> ""
            last.reps > 0 -> last.reps.toString()
            targetReps > 0 -> targetReps.toString()
            else -> ""
        }
        val distanceDraft = when {
            exercise.exercise.type != ExerciseType.DISTANCE -> ""
            last.distance > 0 -> last.distance.toString()
            else -> ""
        }
        val durationDraft = when {
            exercise.exercise.type != ExerciseType.TIME -> ""
            last.duration > 0.0 -> formatDoubleStripped(last.duration)
            else -> ""
        }
        phase.value = Phase.Resting(
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            weightDraft = weightDraft,
            repsDraft = repsDraft,
            distanceDraft = distanceDraft,
            durationDraft = durationDraft,
            side = side,
            isInterSideRest = isFirstSide,
        )
        val alarmName = if (side != null) {
            "${exercise.exercise.name} — ${sideLabel(side)}"
        } else {
            exercise.exercise.name
        }
        restTimerAlarm.schedule(totalSeconds, exerciseName = alarmName)
        startTimer(totalSeconds)
    }

    fun updateRestForm(
        weight: String,
        reps: String,
        distance: String,
        duration: String,
    ) {
        val resting = phase.value as? Phase.Resting ?: return
        phase.value = resting.copy(
            weightDraft = weight,
            repsDraft = reps,
            distanceDraft = distance,
            durationDraft = duration,
        )
    }

    /**
     * Persist the current set to the database. Stays in the Resting phase so the
     * timer and alarm continue running — advancing is a separate action.
     */
    fun logSet() {
        val current = state.value as? WorkoutUiState.InSession ?: return
        val resting = current.phase as? Phase.Resting ?: return
        if (resting.isLogged) return
        val exercise = current.currentExercise ?: return

        val (weight, reps, distance, duration) = when (exercise.exercise.type) {
            ExerciseType.STRENGTH -> {
                val w = resting.weightDraft.toDoubleOrNull() ?: return
                val r = resting.repsDraft.toIntOrNull()?.takeIf { it > 0 } ?: return
                if (w < 0.0) return
                LastEntry(weight = w, reps = r)
            }
            ExerciseType.DISTANCE -> {
                val d = resting.distanceDraft.toIntOrNull()?.takeIf { it > 0 } ?: return
                LastEntry(distance = d)
            }
            ExerciseType.TIME -> {
                val t = resting.durationDraft.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return
                LastEntry(duration = t)
            }
        }

        viewModelScope.launch {
            workoutRepository.recordSet(
                sessionId = current.session.id,
                routineExerciseId = exercise.routineExercise.id,
                setIndex = current.currentSetIndex,
                weight = weight,
                reps = reps,
                side = resting.side,
                distanceMeters = if (exercise.exercise.type == ExerciseType.DISTANCE) distance else null,
                durationSeconds = if (exercise.exercise.type == ExerciseType.TIME) duration else null,
            )
            lastSubmitted[exercise.routineExercise.id to resting.side] =
                LastEntry(weight = weight, reps = reps, distance = distance, duration = duration)
            val stillResting = phase.value as? Phase.Resting ?: return@launch
            phase.value = stillResting.copy(isLogged = true)
        }
    }

    /**
     * End the rest and advance the state machine. For unilateral exercises this advances
     * from first side to second side before the regular set/exercise progression kicks in.
     */
    fun continueToNext() {
        val current = state.value as? WorkoutUiState.InSession ?: return
        when (current.phase) {
            is Phase.Resting -> {
                val exercise = current.currentExercise
                if (exercise != null && exercise.exercise.unilateral && current.currentSideIndex == 0) {
                    cancelTimerAndAlarm()
                    position.value = position.value.copy(sideIndex = 1)
                    phase.value = Phase.Working
                } else {
                    advanceFrom(current)
                }
            }
            is Phase.RestingBeforeNextExercise -> {
                cancelTimerAndAlarm()
                position.value = Position(current.currentExerciseIndex + 1, 1, 0)
                phase.value = Phase.Working
            }
            Phase.Working -> Unit
        }
    }

    fun finishSession() {
        val current = state.value as? WorkoutUiState.InSession ?: return
        cancelTimerAndAlarm()
        viewModelScope.launch {
            workoutRepository.finishSession(current.session.id)
            position.value = Position.START
            phase.value = Phase.Working
        }
    }

    private fun advanceFrom(current: WorkoutUiState.InSession) {
        cancelTimerAndAlarm()
        val currentExercise = current.currentExercise ?: return
        val totalSets = currentExercise.routineExercise.targetSets
        when {
            current.currentSetIndex < totalSets -> {
                position.value = Position(current.currentExerciseIndex, current.currentSetIndex + 1, 0)
                phase.value = Phase.Working
            }
            current.currentExerciseIndex < current.exercises.lastIndex -> {
                val nextExercise = current.exercises[current.currentExerciseIndex + 1]
                val restSeconds = currentExercise.effectiveRestBetweenExercisesSeconds
                if (restSeconds <= 0) {
                    position.value = Position(current.currentExerciseIndex + 1, 1, 0)
                    phase.value = Phase.Working
                } else {
                    phase.value = Phase.RestingBeforeNextExercise(
                        totalSeconds = restSeconds,
                        remainingSeconds = restSeconds,
                        nextExerciseName = nextExercise.exercise.name,
                    )
                    restTimerAlarm.schedule(restSeconds, exerciseName = nextExercise.exercise.name)
                    startTimer(restSeconds)
                }
            }
            else -> {
                viewModelScope.launch {
                    workoutRepository.finishSession(current.session.id)
                    position.value = Position.START
                    phase.value = Phase.Working
                }
            }
        }
    }

    private fun startTimer(totalSeconds: Int) {
        cancelTimer()
        timerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1.seconds)
                remaining--
                phase.value = when (val current = phase.value) {
                    is Phase.Resting -> current.copy(remainingSeconds = remaining)
                    is Phase.RestingBeforeNextExercise -> current.copy(remainingSeconds = remaining)
                    Phase.Working -> return@launch
                }
            }
        }
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun cancelTimerAndAlarm() {
        cancelTimer()
        restTimerAlarm.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimerAndAlarm()
    }
}

private fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toLong().toString() else weight.toString()

private fun formatDoubleStripped(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

internal fun sideLabel(side: Side): String = when (side) {
    Side.LEFT -> "Left"
    Side.RIGHT -> "Right"
}
