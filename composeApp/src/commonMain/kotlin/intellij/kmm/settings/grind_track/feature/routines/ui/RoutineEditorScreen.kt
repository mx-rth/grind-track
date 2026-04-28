package intellij.kmm.settings.grind_track.feature.routines.ui

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import kotlinx.datetime.DayOfWeek
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import intellij.kmm.settings.grind_track.core.data.RoutineExerciseWithExercise
import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    routineId: Long,
    onBack: () -> Unit,
    viewModel: RoutineEditorViewModel = koinViewModel { parametersOf(routineId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Edit routine") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val routine = state.routine
            if (routine != null) {
                var nameDraft by remember(routine.id, routine.name) { mutableStateOf(routine.name) }
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = {
                        nameDraft = it
                        viewModel.renameRoutine(it)
                    },
                    label = { Text("Routine name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ScheduleDayPicker(
                    selected = routine.scheduledDays,
                    onToggle = viewModel::toggleScheduledDay,
                )
                NotificationToggleRow(
                    enabled = routine.notificationEnabled,
                    minuteOfDay = routine.notificationMinuteOfDay,
                    hasScheduledDays = routine.scheduledDays.isNotEmpty(),
                    onToggleEnabled = viewModel::setNotificationEnabled,
                    onPickTime = viewModel::setNotificationTime,
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.exercises, key = { it.routineExercise.id }) { item ->
                        RoutineExerciseRow(
                            item = item,
                            onChange = viewModel::updateRoutineExercise,
                            onRemove = { viewModel.removeRoutineExercise(item.routineExercise.id) },
                            onMoveUp = { viewModel.moveUp(item.routineExercise.id) },
                            onMoveDown = { viewModel.moveDown(item.routineExercise.id) },
                        )
                    }
                }
            }

            Button(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add exercise")
            }
        }
    }

    if (showPicker) {
        AddExerciseDialog(
            catalogue = state.catalogue,
            onDismiss = { showPicker = false },
            onCreateExercise = { name, rest, restBetween, then ->
                viewModel.createExercise(name, rest, restBetween, then)
            },
            onAdd = { exerciseId, sets, reps, restOverride, restBetweenOverride ->
                viewModel.addExercise(exerciseId, sets, reps, restOverride, restBetweenOverride)
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationToggleRow(
    enabled: Boolean,
    minuteOfDay: Int?,
    hasScheduledDays: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onPickTime: (Int) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Reminder notification",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = enabled, onCheckedChange = onToggleEnabled)
        }
        if (enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Time: ${formatMinuteOfDay(minuteOfDay)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showPicker = true }) {
                    Text(if (minuteOfDay == null) "Set time" else "Change")
                }
            }
            if (!hasScheduledDays) {
                Text(
                    text = "Pick training days above to receive reminders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (showPicker) {
        TimePickDialog(
            initialMinuteOfDay = minuteOfDay ?: 480,
            onDismiss = { showPicker = false },
            onConfirm = { picked ->
                onPickTime(picked)
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickDialog(
    initialMinuteOfDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val tps = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(tps.hour * 60 + tps.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Reminder time") },
        text = { TimePicker(state = tps) },
    )
}

private fun formatMinuteOfDay(minuteOfDay: Int?): String {
    if (minuteOfDay == null) return "Not set"
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    val mm = if (m < 10) "0$m" else m.toString()
    val hh = if (h < 10) "0$h" else h.toString()
    return "$hh:$mm"
}

@Composable
private fun ScheduleDayPicker(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Training days",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(DayOfWeek.entries) { day ->
                FilterChip(
                    selected = day in selected,
                    onClick = { onToggle(day) },
                    label = {
                        Text(
                            text = when (day) {
                                DayOfWeek.MONDAY -> "Mo"
                                DayOfWeek.TUESDAY -> "Tu"
                                DayOfWeek.WEDNESDAY -> "We"
                                DayOfWeek.THURSDAY -> "Th"
                                DayOfWeek.FRIDAY -> "Fr"
                                DayOfWeek.SATURDAY -> "Sa"
                                DayOfWeek.SUNDAY -> "Su"
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun RoutineExerciseRow(
    item: RoutineExerciseWithExercise,
    onChange: (intellij.kmm.settings.grind_track.core.database.entity.RoutineExercise) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val re = item.routineExercise
    var setsDraft by remember(re.id) { mutableStateOf(re.targetSets.toString()) }
    var repsDraft by remember(re.id) { mutableStateOf(re.targetReps?.toString() ?: "") }
    var restDraft by remember(re.id) {
        mutableStateOf(re.restSecondsOverride?.toString() ?: "")
    }
    var restBetweenDraft by remember(re.id) {
        mutableStateOf(re.restBetweenExercisesOverride?.toString() ?: "")
    }
    val toFailure = re.targetReps == null

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    label = "Sets",
                    value = setsDraft,
                    onValueChange = { newValue ->
                        setsDraft = newValue
                        newValue.toIntOrNull()?.takeIf { it > 0 }?.let { sets ->
                            if (sets != re.targetSets) onChange(re.copy(targetSets = sets))
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    label = "Reps",
                    value = if (toFailure) "" else repsDraft,
                    onValueChange = { newValue ->
                        repsDraft = newValue
                        newValue.toIntOrNull()?.takeIf { it > 0 }?.let { reps ->
                            if (reps != re.targetReps) onChange(re.copy(targetReps = reps))
                        }
                    },
                    enabled = !toFailure,
                    placeholder = if (toFailure) "Failure" else null,
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    label = "Rest (s)",
                    value = restDraft,
                    onValueChange = { newValue ->
                        restDraft = newValue
                        val parsed = newValue.toIntOrNull()
                        if (newValue.isBlank()) {
                            if (re.restSecondsOverride != null) onChange(re.copy(restSecondsOverride = null))
                        } else if (parsed != null && parsed > 0 && parsed != re.restSecondsOverride) {
                            onChange(re.copy(restSecondsOverride = parsed))
                        }
                    },
                    placeholder = "${item.exercise.defaultRestSeconds}",
                    modifier = Modifier.weight(1f),
                )
            }
            NumberField(
                label = "Rest after exercise (s)",
                value = restBetweenDraft,
                onValueChange = { newValue ->
                    restBetweenDraft = newValue
                    val parsed = newValue.toIntOrNull()
                    if (newValue.isBlank()) {
                        if (re.restBetweenExercisesOverride != null) {
                            onChange(re.copy(restBetweenExercisesOverride = null))
                        }
                    } else if (parsed != null && parsed >= 0 && parsed != re.restBetweenExercisesOverride) {
                        onChange(re.copy(restBetweenExercisesOverride = parsed))
                    }
                },
                placeholder = "${item.exercise.defaultRestBetweenExercisesSeconds}",
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = toFailure,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onChange(re.copy(targetReps = null))
                        } else {
                            val resolved = repsDraft.toIntOrNull()?.takeIf { it > 0 } ?: 8
                            repsDraft = resolved.toString()
                            onChange(re.copy(targetReps = resolved))
                        }
                    },
                )
                Text(
                    text = "To failure (max reps)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
        singleLine = true,
        enabled = enabled,
        modifier = modifier,
    )
}

@Composable
private fun AddExerciseDialog(
    catalogue: List<Exercise>,
    onDismiss: () -> Unit,
    onCreateExercise: (name: String, defaultRest: Int, defaultRestBetween: Int, onCreated: (Long) -> Unit) -> Unit,
    onAdd: (exerciseId: Long, sets: Int, reps: Int?, restOverride: Int?, restBetweenOverride: Int?) -> Unit,
) {
    var step by remember { mutableStateOf<PickerStep>(PickerStep.List) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            val current = step
            Text(
                when (current) {
                    PickerStep.List -> "Pick an exercise"
                    is PickerStep.Configure -> current.exercise.name
                    PickerStep.Create -> "New exercise"
                }
            )
        },
        text = {
            when (val current = step) {
                PickerStep.List -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { step = PickerStep.Create },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Create new exercise") }
                        if (catalogue.isEmpty()) {
                            Text(
                                "No exercises yet — create one above.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(catalogue, key = { it.id }) { exercise ->
                                    TextButton(
                                        onClick = { step = PickerStep.Configure(exercise) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(exercise.name, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }
                is PickerStep.Configure -> ConfigureExerciseForm(
                    exercise = current.exercise,
                    onCancel = { step = PickerStep.List },
                    onAdd = { sets, reps, restOverride, restBetweenOverride ->
                        onAdd(current.exercise.id, sets, reps, restOverride, restBetweenOverride)
                    },
                )
                PickerStep.Create -> CreateExerciseForm(
                    onCancel = { step = PickerStep.List },
                    onCreateAndAdd = { name, rest, restBetween, sets, reps ->
                        onCreateExercise(name, rest, restBetween) { newId ->
                            onAdd(newId, sets, reps, null, null)
                        }
                    },
                )
            }
        },
    )
}

private sealed class PickerStep {
    data object List : PickerStep()
    data class Configure(val exercise: Exercise) : PickerStep()
    data object Create : PickerStep()
}

@Composable
private fun ConfigureExerciseForm(
    exercise: Exercise,
    onCancel: () -> Unit,
    onAdd: (sets: Int, reps: Int?, restOverride: Int?, restBetweenOverride: Int?) -> Unit,
) {
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("8") }
    var toFailure by remember { mutableStateOf(false) }
    var rest by remember { mutableStateOf("") }
    var restBetween by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Sets", sets, { sets = it }, modifier = Modifier.weight(1f))
            NumberField(
                label = "Reps",
                value = if (toFailure) "" else reps,
                onValueChange = { reps = it },
                enabled = !toFailure,
                placeholder = if (toFailure) "Failure" else null,
                modifier = Modifier.weight(1f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = toFailure, onCheckedChange = { toFailure = it })
            Text("To failure (max reps)", style = MaterialTheme.typography.bodyMedium)
        }
        NumberField(
            label = "Rest (s)",
            value = rest,
            onValueChange = { rest = it },
            placeholder = "${exercise.defaultRestSeconds}",
            modifier = Modifier.fillMaxWidth(),
        )
        NumberField(
            label = "Rest after exercise (s)",
            value = restBetween,
            onValueChange = { restBetween = it },
            placeholder = "${exercise.defaultRestBetweenExercisesSeconds}",
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel) { Text("Back") }
            Button(
                enabled = sets.toIntOrNull()?.let { it > 0 } == true &&
                    (toFailure || reps.toIntOrNull()?.let { it > 0 } == true),
                onClick = {
                    val s = sets.toInt()
                    val r = if (toFailure) null else reps.toInt()
                    val ro = rest.toIntOrNull()?.takeIf { it > 0 }
                    val rbo = restBetween.toIntOrNull()?.takeIf { it >= 0 }
                    onAdd(s, r, ro, rbo)
                },
            ) { Text("Add") }
        }
    }
}

@Composable
private fun CreateExerciseForm(
    onCancel: () -> Unit,
    onCreateAndAdd: (name: String, defaultRest: Int, defaultRestBetween: Int, sets: Int, reps: Int?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var rest by remember { mutableStateOf("90") }
    var restBetween by remember { mutableStateOf("180") }
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("8") }
    var toFailure by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        NumberField(
            label = "Default rest (s)",
            value = rest,
            onValueChange = { rest = it },
            modifier = Modifier.fillMaxWidth(),
        )
        NumberField(
            label = "Default rest after exercise (s)",
            value = restBetween,
            onValueChange = { restBetween = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Sets", sets, { sets = it }, modifier = Modifier.weight(1f))
            NumberField(
                label = "Reps",
                value = if (toFailure) "" else reps,
                onValueChange = { reps = it },
                enabled = !toFailure,
                placeholder = if (toFailure) "Failure" else null,
                modifier = Modifier.weight(1f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = toFailure, onCheckedChange = { toFailure = it })
            Text("To failure (max reps)", style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel) { Text("Back") }
            Button(
                enabled = name.isNotBlank() &&
                    rest.toIntOrNull()?.let { it > 0 } == true &&
                    restBetween.toIntOrNull()?.let { it >= 0 } == true &&
                    sets.toIntOrNull()?.let { it > 0 } == true &&
                    (toFailure || reps.toIntOrNull()?.let { it > 0 } == true),
                onClick = {
                    val resolvedReps = if (toFailure) null else reps.toInt()
                    onCreateAndAdd(name.trim(), rest.toInt(), restBetween.toInt(), sets.toInt(), resolvedReps)
                },
            ) { Text("Create & add") }
        }
    }
}
