package intellij.kmm.settings.grind_track.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import intellij.kmm.settings.grind_track.core.data.RoutineExerciseWithExercise
import intellij.kmm.settings.grind_track.core.data.RoutineRepository
import intellij.kmm.settings.grind_track.core.data.WorkoutRepository
import intellij.kmm.settings.grind_track.core.database.entity.Routine
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
        val phase: Phase,
    ) : WorkoutUiState {
        val currentExercise: RoutineExerciseWithExercise?
            get() = exercises.getOrNull(currentExerciseIndex)

        val isLastSetOfExercise: Boolean
            get() = currentSetIndex >= (currentExercise?.routineExercise?.targetSets ?: 0)

        val isLastExercise: Boolean
            get() = currentExerciseIndex >= exercises.lastIndex
    }
}

private data class Position(val exerciseIndex: Int, val setIndex: Int) {
    companion object {
        val START = Position(0, 1)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
) : ViewModel() {

    private val position = MutableStateFlow(Position.START)
    private val phase = MutableStateFlow<Phase>(Phase.Working)

    /** Last submitted weight/reps per `routineExerciseId`, used to prefill the rest form. */
    private val lastSubmitted = mutableMapOf<Long, Pair<Double, Int>>()

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
        val totalSeconds = exercise.effectiveRestSeconds
        val (weightPrefill, repsPrefill) = lastSubmitted[exercise.routineExercise.id]
            ?: (0.0 to (exercise.routineExercise.targetReps ?: 0))
        phase.value = Phase.Resting(
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            weightDraft = if (weightPrefill > 0.0) formatWeight(weightPrefill) else "",
            repsDraft = if (repsPrefill > 0) repsPrefill.toString() else "",
        )
        startTimer(totalSeconds)
    }

    fun updateRestForm(weight: String, reps: String) {
        val resting = phase.value as? Phase.Resting ?: return
        phase.value = resting.copy(weightDraft = weight, repsDraft = reps)
    }

    /** Submit the rest form: write SetEntry, advance state machine, return to Working. */
    fun submitRest() {
        val current = state.value as? WorkoutUiState.InSession ?: return
        val resting = current.phase as? Phase.Resting ?: return
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
            )
            lastSubmitted[exercise.routineExercise.id] = weight to reps
            advanceFrom(current)
        }
    }

    /** Skip logging this set: advance without writing a SetEntry. */
    fun skipRest() {
        val current = state.value as? WorkoutUiState.InSession ?: return
        if (current.phase !is Phase.Resting) return
        advanceFrom(current)
    }

    fun finishSession() {
        val current = state.value as? WorkoutUiState.InSession ?: return
        cancelTimer()
        viewModelScope.launch {
            workoutRepository.finishSession(current.session.id)
            position.value = Position.START
            phase.value = Phase.Working
        }
    }

    private fun advanceFrom(current: WorkoutUiState.InSession) {
        cancelTimer()
        val totalSets = current.currentExercise?.routineExercise?.targetSets ?: return
        when {
            current.currentSetIndex < totalSets -> {
                position.value = Position(current.currentExerciseIndex, current.currentSetIndex + 1)
                phase.value = Phase.Working
            }
            current.currentExerciseIndex < current.exercises.lastIndex -> {
                position.value = Position(current.currentExerciseIndex + 1, 1)
                phase.value = Phase.Working
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
                val resting = phase.value as? Phase.Resting ?: return@launch
                phase.value = resting.copy(remainingSeconds = remaining)
            }
        }
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimer()
    }
}

private fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toLong().toString() else weight.toString()
