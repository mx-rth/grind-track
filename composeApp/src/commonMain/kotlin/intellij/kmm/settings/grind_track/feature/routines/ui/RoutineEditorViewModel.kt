package intellij.kmm.settings.grind_track.feature.routines.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import intellij.kmm.settings.grind_track.core.data.ExerciseRepository
import intellij.kmm.settings.grind_track.core.data.RoutineExerciseWithExercise
import intellij.kmm.settings.grind_track.core.data.RoutineRepository
import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import intellij.kmm.settings.grind_track.core.database.entity.RoutineExercise
import kotlinx.datetime.DayOfWeek
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutineEditorUiState(
    val routine: Routine? = null,
    val exercises: List<RoutineExerciseWithExercise> = emptyList(),
    val catalogue: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
)

class RoutineEditorViewModel(
    private val routineId: Long,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    val state: StateFlow<RoutineEditorUiState> = combine(
        routineRepository.observeRoutine(routineId),
        routineRepository.observeExercisesForRoutine(routineId),
        exerciseRepository.observeExercises(),
    ) { routine, exercises, catalogue ->
        RoutineEditorUiState(
            routine = routine,
            exercises = exercises,
            catalogue = catalogue,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineEditorUiState())

    fun renameRoutine(newName: String) {
        val current = state.value.routine ?: return
        if (current.name == newName) return
        viewModelScope.launch { routineRepository.renameRoutine(current, newName) }
    }

    fun toggleScheduledDay(day: DayOfWeek) {
        val current = state.value.routine ?: return
        val newDays = if (day in current.scheduledDays) current.scheduledDays - day
                      else current.scheduledDays + day
        viewModelScope.launch { routineRepository.setScheduledDays(current, newDays) }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        val current = state.value.routine ?: return
        if (current.notificationEnabled == enabled) return
        viewModelScope.launch { routineRepository.setNotificationEnabled(current, enabled) }
    }

    fun setNotificationTime(minuteOfDay: Int) {
        val current = state.value.routine ?: return
        if (current.notificationMinuteOfDay == minuteOfDay) return
        viewModelScope.launch { routineRepository.setNotificationMinuteOfDay(current, minuteOfDay) }
    }

    fun addExercise(
        exerciseId: Long,
        targetSets: Int,
        targetReps: Int?,
        restSecondsOverride: Int?,
        restBetweenExercisesOverride: Int?,
        restAfterFirstSideSecondsOverride: Int? = null,
        startingSide: intellij.kmm.settings.grind_track.core.database.entity.Side =
            intellij.kmm.settings.grind_track.core.database.entity.Side.LEFT,
    ) {
        viewModelScope.launch {
            routineRepository.addExerciseToRoutine(
                routineId = routineId,
                exerciseId = exerciseId,
                targetSets = targetSets,
                targetReps = targetReps,
                restSecondsOverride = restSecondsOverride,
                restBetweenExercisesOverride = restBetweenExercisesOverride,
                restAfterFirstSideSecondsOverride = restAfterFirstSideSecondsOverride,
                startingSide = startingSide,
            )
        }
    }

    fun updateRoutineExercise(routineExercise: RoutineExercise) {
        viewModelScope.launch { routineRepository.updateRoutineExercise(routineExercise) }
    }

    fun removeRoutineExercise(id: Long) {
        viewModelScope.launch { routineRepository.removeRoutineExercise(id) }
    }

    fun moveUp(id: Long) {
        viewModelScope.launch { routineRepository.moveUp(id) }
    }

    fun moveDown(id: Long) {
        viewModelScope.launch { routineRepository.moveDown(id) }
    }

    fun createExercise(
        name: String,
        defaultRestSeconds: Int,
        defaultRestBetweenExercisesSeconds: Int,
        unilateral: Boolean = false,
        defaultRestAfterFirstSideSeconds: Int = 60,
        bodyWeight: Boolean = false,
        onCreated: (Long) -> Unit,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = exerciseRepository.create(
                name = name,
                defaultRestSeconds = defaultRestSeconds,
                defaultRestBetweenExercisesSeconds = defaultRestBetweenExercisesSeconds,
                unilateral = unilateral,
                defaultRestAfterFirstSideSeconds = defaultRestAfterFirstSideSeconds,
                bodyWeight = bodyWeight,
            )
            onCreated(id)
        }
    }
}
