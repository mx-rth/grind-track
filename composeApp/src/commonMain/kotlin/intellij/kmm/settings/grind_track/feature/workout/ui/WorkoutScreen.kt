package intellij.kmm.settings.grind_track.feature.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import intellij.kmm.settings.grind_track.core.data.RoutineExerciseWithExercise
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import intellij.kmm.settings.grind_track.core.designsystem.EmptyState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (val current = state) {
                            is WorkoutUiState.InSession -> current.routine.name.ifBlank { "Workout" }
                            else -> "Workout"
                        }
                    )
                },
                actions = {
                    if (state is WorkoutUiState.InSession) {
                        TextButton(onClick = viewModel::finishSession) { Text("Finish") }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                WorkoutUiState.Loading -> Unit
                is WorkoutUiState.Picking -> RoutinePicker(
                    routines = current.routines,
                    onPick = viewModel::startSession,
                )
                is WorkoutUiState.InSession -> InSessionContent(
                    state = current,
                    onMarkSetComplete = viewModel::markSetComplete,
                    onUpdateRestForm = viewModel::updateRestForm,
                    onLogSet = viewModel::logSet,
                    onContinueToNext = viewModel::continueToNext,
                    onFinish = viewModel::finishSession,
                )
            }
        }
    }
}

@Composable
private fun RoutinePicker(
    routines: List<Routine>,
    onPick: (Long) -> Unit,
) {
    if (routines.isEmpty()) {
        EmptyState(
            title = "No routines yet",
            subtitle = "Create a routine in the Routines tab to start a workout.",
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Pick a routine to start",
            style = MaterialTheme.typography.titleMedium,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(routines, key = { it.id }) { routine ->
                RoutinePickerRow(routine = routine, onClick = { onPick(routine.id) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutinePickerRow(routine: Routine, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = routine.name.ifBlank { "Untitled routine" },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun InSessionContent(
    state: WorkoutUiState.InSession,
    onMarkSetComplete: () -> Unit,
    onUpdateRestForm: (weight: String, reps: String) -> Unit,
    onLogSet: () -> Unit,
    onContinueToNext: () -> Unit,
    onFinish: () -> Unit,
) {
    val current = state.currentExercise
    if (current == null) {
        EmptyExerciseList(onFinish = onFinish)
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Exercise ${state.currentExerciseIndex + 1} of ${state.exercises.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = current.exercise.name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Set ${state.currentSetIndex} of ${current.routineExercise.targetSets}",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = repsLabel(current),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Box(modifier = Modifier.weight(1f)) {
            when (val phase = state.phase) {
                Phase.Working -> WorkingControls(
                    restSeconds = current.effectiveRestSeconds,
                    onMarkSetComplete = onMarkSetComplete,
                    onFinish = onFinish,
                )
                is Phase.Resting -> RestingControls(
                    phase = phase,
                    onUpdateForm = onUpdateRestForm,
                    onLogSet = onLogSet,
                    onContinue = onContinueToNext,
                )
            }
        }
    }
}

@Composable
private fun WorkingControls(
    restSeconds: Int,
    onMarkSetComplete: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = "Rest will be ${restSeconds}s",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onMarkSetComplete,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Text("Mark set complete")
        }
        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Abandon workout")
        }
    }
}

@Composable
private fun RestingControls(
    phase: Phase.Resting,
    onUpdateForm: (weight: String, reps: String) -> Unit,
    onLogSet: () -> Unit,
    onContinue: () -> Unit,
) {
    val timerLabel = when {
        phase.remainingSeconds > 0 -> "Rest: ${phase.remainingSeconds}s"
        else -> "Rest complete"
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = timerLabel,
            style = MaterialTheme.typography.titleLarge,
            color = if (phase.remainingSeconds > 0)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = phase.weightDraft,
                onValueChange = { onUpdateForm(it, phase.repsDraft) },
                label = { Text("Weight") },
                enabled = !phase.isLogged,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = phase.repsDraft,
                onValueChange = { onUpdateForm(phase.weightDraft, it) },
                label = { Text("Reps") },
                enabled = !phase.isLogged,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Box(modifier = Modifier.weight(1f))
        Button(
            onClick = onLogSet,
            enabled = !phase.isLogged && isSubmittable(phase),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (phase.isLogged) "Logged ✓" else "Log set")
        }
        OutlinedButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (phase.isLogged) "Continue to next set" else "Skip & continue")
        }
    }
}

@Composable
private fun EmptyExerciseList(onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "This routine has no exercises yet.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Box(modifier = Modifier.padding(top = 16.dp)) {
            Button(onClick = onFinish) { Text("End session") }
        }
    }
}

private fun repsLabel(item: RoutineExerciseWithExercise): String {
    val reps = item.routineExercise.targetReps
    return if (reps == null) "Reps: to failure" else "Target: $reps reps"
}

private fun isSubmittable(phase: Phase.Resting): Boolean {
    val weight = phase.weightDraft.toDoubleOrNull() ?: return false
    val reps = phase.repsDraft.toIntOrNull() ?: return false
    return weight >= 0.0 && reps > 0
}
