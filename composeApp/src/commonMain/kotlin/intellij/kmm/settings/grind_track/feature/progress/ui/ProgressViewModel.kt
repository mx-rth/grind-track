package intellij.kmm.settings.grind_track.feature.progress.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import intellij.kmm.settings.grind_track.core.data.ProgressRepository
import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import intellij.kmm.settings.grind_track.core.database.entity.SetEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(
    val exercises: List<Exercise> = emptyList(),
    val selectedExerciseId: Long? = null,
    val history: List<SetEntry> = emptyList(),
    val streak: Int = 0,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    private val repository: ProgressRepository,
) : ViewModel() {

    private val explicitSelection = MutableStateFlow<Long?>(null)

    val state: StateFlow<ProgressUiState> = combine(
        repository.observeExercisesWithActivity(),
        repository.observeStreak(),
        explicitSelection,
    ) { exercises, streak, explicit ->
        Triple(explicit ?: exercises.firstOrNull()?.id, exercises, streak)
    }.flatMapLatest { triple ->
        val effectiveId = triple.first
        val exercises = triple.second
        val streak = triple.third
        if (effectiveId == null) {
            flowOf(ProgressUiState(exercises = exercises, streak = streak, isLoading = false))
        } else {
            repository.observeHistoryForExercise(effectiveId).map { history ->
                ProgressUiState(
                    exercises = exercises,
                    selectedExerciseId = effectiveId,
                    history = history,
                    streak = streak,
                    isLoading = false,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun selectExercise(exerciseId: Long) {
        explicitSelection.value = exerciseId
    }
}
