package intellij.kmm.settings.grind_track.feature.routines.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import intellij.kmm.settings.grind_track.core.data.RoutineRepository
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutinesUiState(
    val routines: List<Routine> = emptyList(),
    val isLoading: Boolean = true,
)

class RoutinesViewModel(
    private val repository: RoutineRepository,
) : ViewModel() {
    val state: StateFlow<RoutinesUiState> = repository.observeRoutines()
        .map { RoutinesUiState(routines = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutinesUiState())

    private val _newRoutineId = MutableStateFlow<Long?>(null)
    val newRoutineId: StateFlow<Long?> = _newRoutineId.asStateFlow()

    fun createRoutine() {
        viewModelScope.launch {
            val id = repository.createRoutine(name = "New routine")
            _newRoutineId.value = id
        }
    }

    fun consumeNewRoutineId() {
        _newRoutineId.value = null
    }

    fun deleteRoutine(id: Long) {
        viewModelScope.launch { repository.deleteRoutine(id) }
    }
}
