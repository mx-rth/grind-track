package intellij.kmm.settings.grind_track.feature.progress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import intellij.kmm.settings.grind_track.core.database.entity.SetEntry
import intellij.kmm.settings.grind_track.core.designsystem.EmptyState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Progress") }) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> Unit
                state.exercises.isEmpty() -> EmptyState(
                    title = "No data yet",
                    subtitle = "Complete a workout to start seeing your progress here.",
                )
                else -> LoadedContent(
                    state = state,
                    onSelectExercise = viewModel::selectExercise,
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(
    state: ProgressUiState,
    onSelectExercise: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ExercisePicker(
            exercises = state.exercises,
            selectedExerciseId = state.selectedExerciseId,
            onSelect = onSelectExercise,
        )
        HorizontalDivider()
        if (state.history.isEmpty()) {
            EmptyState(
                title = "No sets logged for this exercise",
                subtitle = "Log a set in the Workout tab to see it here.",
            )
        } else {
            HistoryList(history = state.history)
        }
    }
}

@Composable
private fun ExercisePicker(
    exercises: List<Exercise>,
    selectedExerciseId: Long?,
    onSelect: (Long) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(exercises, key = { it.id }) { exercise ->
            FilterChip(
                selected = exercise.id == selectedExerciseId,
                onClick = { onSelect(exercise.id) },
                label = { Text(exercise.name) },
            )
        }
    }
}

@Composable
private fun HistoryList(history: List<SetEntry>) {
    val timeZone = TimeZone.currentSystemDefault()
    val grouped = history.groupBy { entry ->
        entry.completedAt.toLocalDateTime(timeZone).date
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        grouped.forEach { (date, entries) ->
            item(key = date.toString()) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = formatDate(date),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        entries.forEachIndexed { idx, entry ->
                            SetEntryRow(index = idx + 1, entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetEntryRow(index: Int, entry: SetEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Set $index",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${formatWeight(entry.weight)} × ${entry.reps}",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toLong().toString() else weight.toString()

private fun formatDate(date: kotlinx.datetime.LocalDate): String {
    val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$month ${date.day}, ${date.year}"
}
