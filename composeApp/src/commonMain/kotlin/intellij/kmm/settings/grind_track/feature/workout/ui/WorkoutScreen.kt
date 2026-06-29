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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import intellij.kmm.settings.grind_track.core.designsystem.AccentBadge
import intellij.kmm.settings.grind_track.core.designsystem.BrandColors
import intellij.kmm.settings.grind_track.core.designsystem.EmptyState
import intellij.kmm.settings.grind_track.core.designsystem.HeroCard
import intellij.kmm.settings.grind_track.core.designsystem.Mascot
import intellij.kmm.settings.grind_track.core.designsystem.MascotPose
import intellij.kmm.settings.grind_track.core.designsystem.SectionHeader
import intellij.kmm.settings.grind_track.core.designsystem.StripedCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingFinish by remember { mutableStateOf<FinishKind?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (val current = state) {
                            is WorkoutUiState.InSession -> current.routine.name.ifBlank { "Workout" }
                            else -> "Workout"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                actions = {
                    if (state is WorkoutUiState.InSession) {
                        TextButton(onClick = { pendingFinish = FinishKind.Finish }) { Text("Finish") }
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
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
                    onMarkSetComplete = { viewModel.markSetComplete() },
                    onUpdateRestForm = viewModel::updateRestForm,
                    onLogSet = viewModel::logSet,
                    onContinueToNext = viewModel::continueToNext,
                    onFinish = { pendingFinish = FinishKind.Abandon },
                    onStartStopwatch = viewModel::startStopwatch,
                    onStopStopwatch = viewModel::stopStopwatch,
                    onStartCountdown = viewModel::startCountdown,
                    onCancelCountdown = viewModel::cancelCountdown,
                )
            }

            pendingFinish?.let { kind ->
                FinishConfirmDialog(
                    kind = kind,
                    onConfirm = {
                        pendingFinish = null
                        viewModel.finishSession()
                    },
                    onDismiss = { pendingFinish = null },
                )
            }
        }
    }
}

private enum class FinishKind { Finish, Abandon }

@Composable
private fun FinishConfirmDialog(
    kind: FinishKind,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, body, confirmLabel) = when (kind) {
        FinishKind.Finish -> Triple(
            "Finish workout?",
            "End this workout session now? Sets you've already logged stay saved.",
            "Finish",
        )
        FinishKind.Abandon -> Triple(
            "Abandon workout?",
            "End this workout now and skip the rest of the routine? Sets you've already logged stay saved.",
            "Abandon",
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (kind == FinishKind.Abandon) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep going") }
        },
    )
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "hero") {
            HeroCard(
                color = BrandColors.Coral,
                onColor = androidx.compose.ui.graphics.Color.White,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AccentBadge(
                            text = "Up next",
                            container = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                            onContainer = androidx.compose.ui.graphics.Color.White,
                        )
                        Text(
                            text = "Pick a routine,\npress play.",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            text = "${routines.size} ready to go",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Mascot(
                        pose = MascotPose.ReadyToTrain,
                        size = 180.dp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }
        item(key = "section") {
            SectionHeader(text = "Choose a routine")
        }
        items(routines, key = { it.id }) { routine ->
            RoutinePickerRow(routine = routine, onClick = { onPick(routine.id) })
        }
    }
}

@Composable
private fun RoutinePickerRow(routine: Routine, onClick: () -> Unit) {
    val accent = pickerAccentForRoutine(routine.id)
    StripedCard(accent = accent, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(end = 12.dp),
            ) {
                Box(modifier = Modifier.padding(10.dp)) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name.ifBlank { "Untitled routine" },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

private fun pickerAccentForRoutine(id: Long): androidx.compose.ui.graphics.Color {
    val palette = listOf(
        BrandColors.Coral,
        BrandColors.Electric,
        BrandColors.SunYellow,
        BrandColors.MintFresh,
    )
    val idx = ((id % palette.size).toInt() + palette.size) % palette.size
    return palette[idx]
}

@Composable
private fun InSessionContent(
    state: WorkoutUiState.InSession,
    onMarkSetComplete: () -> Unit,
    onUpdateRestForm: (weight: String, reps: String, distance: String, duration: String) -> Unit,
    onLogSet: () -> Unit,
    onContinueToNext: () -> Unit,
    onFinish: () -> Unit,
    onStartStopwatch: () -> Unit,
    onStopStopwatch: () -> Unit,
    onStartCountdown: () -> Unit,
    onCancelCountdown: () -> Unit,
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
        is Phase.Working,
        is Phase.Resting -> Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HeroCard(
                color = MaterialTheme.colorScheme.primaryContainer,
                onColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AccentBadge(
                                text = "Set ${state.currentSetIndex}/${current.routineExercise.targetSets}",
                                container = MaterialTheme.colorScheme.onPrimaryContainer,
                                onContainer = MaterialTheme.colorScheme.primaryContainer,
                            )
                            Box(modifier = Modifier.weight(1f))
                            AccentBadge(
                                text = "Exercise ${state.currentExerciseIndex + 1}/${state.exercises.size}",
                                container = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                                onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Text(
                            text = current.exercise.name,
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AccentBadge(
                                text = targetLabel(current),
                                container = BrandColors.Coral,
                                onContainer = androidx.compose.ui.graphics.Color.White,
                            )
                            val side = state.currentSide
                            if (side != null) {
                                AccentBadge(
                                    text = "${sideLabelText(side)} side",
                                    container = BrandColors.Electric,
                                    onContainer = androidx.compose.ui.graphics.Color.White,
                                )
                            }
                        }
                    }
                    Mascot(
                        pose = if (phase is Phase.Resting) MascotPose.ThumbsUp else MascotPose.HandsOnHips,
                        size = 130.dp,
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (phase) {
                    is Phase.Working -> {
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
                        when (current.exercise.type) {
                            ExerciseType.TIME -> StopwatchControls(
                                phase = phase,
                                restLabel = restLabel,
                                onStart = onStartStopwatch,
                                onStop = onStopStopwatch,
                                onLogManually = onMarkSetComplete,
                                onFinish = onFinish,
                            )
                            ExerciseType.DISTANCE -> {
                                val target = current.routineExercise.targetDurationSeconds
                                if (target != null && target > 0.0) {
                                    CountdownControls(
                                        phase = phase,
                                        targetSeconds = target,
                                        restLabel = restLabel,
                                        onStart = onStartCountdown,
                                        onCancel = onCancelCountdown,
                                        onMarkSetComplete = onMarkSetComplete,
                                        onFinish = onFinish,
                                    )
                                } else {
                                    WorkingControls(
                                        restLabel = restLabel,
                                        onMarkSetComplete = onMarkSetComplete,
                                        onFinish = onFinish,
                                    )
                                }
                            }
                            ExerciseType.STRENGTH -> WorkingControls(
                                restLabel = restLabel,
                                onMarkSetComplete = onMarkSetComplete,
                                onFinish = onFinish,
                            )
                        }
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

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Mascot(
                pose = MascotPose.Clapping,
                size = 220.dp,
            )
        }
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
private fun CountdownControls(
    phase: Phase.Working,
    targetSeconds: Double,
    restLabel: String,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onMarkSetComplete: () -> Unit,
    onFinish: () -> Unit,
) {
    val remaining = phase.countdownRemaining
    val running = remaining != null
    val timeUp = remaining != null && remaining <= 0.0
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatStopwatch(remaining ?: targetSeconds),
            style = MaterialTheme.typography.displayMedium,
            color = when {
                timeUp -> MaterialTheme.colorScheme.tertiary
                running -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (timeUp) {
            Text(
                text = "Time's up — log your distance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = restLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onMarkSetComplete,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Text("Mark set complete")
        }
        if (running) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text("Cancel countdown")
            }
        } else {
            TextButton(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text("Start countdown")
            }
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
private fun StopwatchControls(
    phase: Phase.Working,
    restLabel: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onLogManually: () -> Unit,
    onFinish: () -> Unit,
) {
    val running = phase.stopwatchElapsed != null
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatStopwatch(phase.stopwatchElapsed ?: 0.0),
            style = MaterialTheme.typography.displayMedium,
            color = if (running)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = restLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = if (running) onStop else onStart,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Text(if (running) "Stop & log" else "Start stopwatch")
        }
        if (!running) {
            TextButton(
                onClick = onLogManually,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text("Enter time manually")
            }
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
        phase.totalSeconds > 0 -> "$restLabelPrefix complete"
        else -> null
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
        if (timerLabel != null) {
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
        }
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
        Mascot(
            pose = MascotPose.Standing,
            size = 180.dp,
        )
        Text(
            "This routine has no exercises yet.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
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
        w != null && w >= 0.0 && r != null && r >= 0
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
