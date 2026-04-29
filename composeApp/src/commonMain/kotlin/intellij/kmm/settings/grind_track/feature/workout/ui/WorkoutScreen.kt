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
import intellij.kmm.settings.grind_track.core.database.entity.ExerciseType
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import intellij.kmm.settings.grind_track.core.database.entity.Side
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
    onUpdateRestForm: (weight: String, reps: String, distance: String, duration: String) -> Unit,
    onLogSet: () -> Unit,
    onContinueToNext: () -> Unit,
    onFinish: () -> Unit,
) {
    val current = state.currentExercise
    if (current == null) {
        EmptyExerciseList(onFinish = onFinish)
        return
    }
    when (val phase = state.phase) {
        is Phase.RestingBeforeNextExercise -> RestingBeforeNextExerciseContent(
            phase = phase,
            onContinue = onContinueToNext,
            onFinish = onFinish,
        )
        Phase.Working,
        is Phase.Resting -> Column(
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
            val side = state.currentSide
            if (side != null) {
                Text(
                    text = "${sideLabelText(side)} side",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Text(
                text = targetLabel(current),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Box(modifier = Modifier.weight(1f)) {
                when (phase) {
                    Phase.Working -> {
                        val isUnilateralFirstSide = current.exercise.unilateral && state.currentSideIndex == 0
                        val restSeconds = if (isUnilateralFirstSide) {
                            current.effectiveRestAfterFirstSideSeconds
                        } else {
                            current.effectiveRestSeconds
                        }
                        val restLabel = if (isUnilateralFirstSide) {
                            "Rest between sides will be ${restSeconds}s"
                        } else {
                            "Rest will be ${restSeconds}s"
                        }
                        WorkingControls(
                            restLabel = restLabel,
                            onMarkSetComplete = onMarkSetComplete,
                            onFinish = onFinish,
                        )
                    }
                    is Phase.Resting -> RestingControls(
                        phase = phase,
                        type = current.exercise.type,
                        weightLabel = if (current.exercise.bodyWeight) "Added weight" else "Weight",
                        onUpdateForm = onUpdateRestForm,
                        onLogSet = onLogSet,
                        onContinue = onContinueToNext,
                    )
                    is Phase.RestingBeforeNextExercise -> Unit
                }
            }
        }
    }
}

private fun sideLabelText(side: Side): String = when (side) {
    Side.LEFT -> "Left"
    Side.RIGHT -> "Right"
}

@Composable
private fun RestingBeforeNextExerciseContent(
    phase: Phase.RestingBeforeNextExercise,
    onContinue: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Up next",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = phase.nextExerciseName,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        val timerLabel = if (phase.remainingSeconds > 0) {
            "Rest: ${phase.remainingSeconds}s"
        } else {
            "Rest complete"
        }
        Text(
            text = timerLabel,
            style = MaterialTheme.typography.titleLarge,
            color = if (phase.remainingSeconds > 0)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface,
        )

        Box(modifier = Modifier.weight(1f))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (phase.remainingSeconds > 0) "Skip rest" else "Start next exercise")
        }
        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Abandon workout")
        }
    }
}

@Composable
private fun WorkingControls(
    restLabel: String,
    onMarkSetComplete: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = restLabel,
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
    type: ExerciseType,
    weightLabel: String,
    onUpdateForm: (weight: String, reps: String, distance: String, duration: String) -> Unit,
    onLogSet: () -> Unit,
    onContinue: () -> Unit,
) {
    val restLabelPrefix = if (phase.isInterSideRest) "Rest before next side" else "Rest"
    val timerLabel = when {
        phase.remainingSeconds > 0 -> "$restLabelPrefix: ${phase.remainingSeconds}s"
        else -> "$restLabelPrefix complete"
    }
    val continueLabel = when {
        phase.isInterSideRest && phase.isLogged -> "Continue to next side"
        phase.isInterSideRest -> "Skip & continue to next side"
        phase.isLogged -> "Continue to next set"
        else -> "Skip & continue"
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (phase.side != null) {
            Text(
                text = "Logging: ${sideLabelText(phase.side)} side",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
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
        when (type) {
            ExerciseType.STRENGTH -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phase.weightDraft,
                    onValueChange = {
                        onUpdateForm(it, phase.repsDraft, phase.distanceDraft, phase.durationDraft)
                    },
                    label = { Text(weightLabel) },
                    enabled = !phase.isLogged,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = phase.repsDraft,
                    onValueChange = {
                        onUpdateForm(phase.weightDraft, it, phase.distanceDraft, phase.durationDraft)
                    },
                    label = { Text("Reps") },
                    enabled = !phase.isLogged,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            ExerciseType.DISTANCE -> OutlinedTextField(
                value = phase.distanceDraft,
                onValueChange = {
                    onUpdateForm(phase.weightDraft, phase.repsDraft, it, phase.durationDraft)
                },
                label = { Text("Distance (m)") },
                enabled = !phase.isLogged,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ExerciseType.TIME -> OutlinedTextField(
                value = phase.durationDraft,
                onValueChange = {
                    onUpdateForm(phase.weightDraft, phase.repsDraft, phase.distanceDraft, it)
                },
                label = { Text("Time (s)") },
                enabled = !phase.isLogged,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(modifier = Modifier.weight(1f))
        Button(
            onClick = onLogSet,
            enabled = !phase.isLogged && isSubmittable(phase, type),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (phase.isLogged) "Logged ✓" else "Log set")
        }
        OutlinedButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(continueLabel)
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

private fun targetLabel(item: RoutineExerciseWithExercise): String {
    return when (item.exercise.type) {
        ExerciseType.STRENGTH -> {
            val reps = item.routineExercise.targetReps
            if (reps == null) "Reps: to failure" else "Target: $reps reps"
        }
        ExerciseType.DISTANCE -> {
            val seconds = item.routineExercise.targetDurationSeconds
            if (seconds == null) "Run as long as you can" else "Target time: ${formatDurationSeconds(seconds)}"
        }
        ExerciseType.TIME -> {
            val meters = item.routineExercise.targetDistanceMeters
            if (meters == null) "Cover the distance" else "Target distance: ${formatMeters(meters)}"
        }
    }
}

private fun isSubmittable(phase: Phase.Resting, type: ExerciseType): Boolean = when (type) {
    ExerciseType.STRENGTH -> {
        val w = phase.weightDraft.toDoubleOrNull()
        val r = phase.repsDraft.toIntOrNull()
        w != null && w >= 0.0 && r != null && r > 0
    }
    ExerciseType.DISTANCE -> {
        val d = phase.distanceDraft.toIntOrNull()
        d != null && d > 0
    }
    ExerciseType.TIME -> {
        val t = phase.durationDraft.toDoubleOrNull()
        t != null && t > 0.0
    }
}

internal fun formatDurationSeconds(seconds: Double): String {
    if (seconds < 60.0) {
        return if (seconds % 1.0 == 0.0) "${seconds.toInt()}s" else "${seconds}s"
    }
    val totalWholeSeconds = seconds.toInt()
    val mins = totalWholeSeconds / 60
    val secs = totalWholeSeconds - mins * 60
    val secsStr = if (secs < 10) "0$secs" else secs.toString()
    return "$mins:$secsStr"
}

internal fun formatMeters(meters: Int): String =
    if (meters >= 1000) {
        val km = meters / 1000.0
        val s = if (km % 1.0 == 0.0) km.toInt().toString() else km.toString()
        "$s km"
    } else {
        "$meters m"
    }
