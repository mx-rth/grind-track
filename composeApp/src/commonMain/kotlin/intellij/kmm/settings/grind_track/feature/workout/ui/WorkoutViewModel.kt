package intellij.kmm.settings.grind_track.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import intellij.kmm.settings.grind_track.core.data.RoutineExerciseWithExercise
import intellij.kmm.settings.grind_track.core.data.RoutineRepository
import intellij.kmm.settings.grind_track.core.data.WorkoutRepository
import intellij.kmm.settings.grind_track.core.notifications.RestTimerAlarm
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
        val weightDraft: String,
        val repsDraft: String,
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

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val restTimerAlarm: RestTimerAlarm,
) : ViewModel() {

    private val position = MutableStateFlow(Position.START)
    private val phase = MutableStateFlow<Phase>(Phase.Working)

    /** Last submitted weight/reps per `(routineExerciseId, side?)`, used to prefill the rest form. */
    private val lastSubmitted = mutableMapOf<Pair<Long, Side?>, Pair<Double, Int>>()

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
        val (weightPrefill, repsPrefill) = lastSubmitted[exercise.routineExercise.id to side]
            ?: (0.0 to (exercise.routineExercise.targetReps ?: 0))
        val weightDraft = when {
            weightPrefill > 0.0 -> formatWeight(weightPrefill)
            exercise.exercise.bodyWeight -> "0"
            else -> ""
        }
        phase.value = Phase.Resting(
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            weightDraft = weightDraft,
            repsDraft = if (repsPrefill > 0) repsPrefill.toString() else "",
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

    fun updateRestForm(weight: String, reps: String) {
        val resting = phase.value as? Phase.Resting ?: return
        phase.value = resting.copy(weightDraft = weight, repsDraft = reps)
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
        val weight = resting.weightDraft.toDoubleOrNull() ?: return
        val reps = resting.repsDraft.toIntOrNull()?.takeIf { it > 0 } ?: return
        if (weight < 0.0) return

        viewModelScope.launch {
            workoutRepository.recordSet(
                sessionId = current.session.id,
                routineExerciseId = exercise.routineExercise.id,
                setIndex = current.currentSetIndex,
                weight = weight,
                reps = reps,
                side = resting.side,
            )
            lastSubmitted[exercise.routineExercise.id to resting.side] = weight to reps
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

internal fun sideLabel(side: Side): String = when (side) {
    Side.LEFT -> "Left"
    Side.RIGHT -> "Right"
}
