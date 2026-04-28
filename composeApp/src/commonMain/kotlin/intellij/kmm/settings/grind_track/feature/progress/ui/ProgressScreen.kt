package intellij.kmm.settings.grind_track.feature.progress.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import intellij.kmm.settings.grind_track.core.database.entity.SetEntry
import intellij.kmm.settings.grind_track.core.designsystem.EmptyState
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate
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
    val sessionGroups = history
        .groupBy { it.sessionId }
        .entries
        .sortedBy { (_, entries) -> entries.minOf { it.completedAt } }
    val dateOccurrences = mutableMapOf<LocalDate, Int>()
    val sessionLabels: List<String> = sessionGroups.map { (_, entries) ->
        val date = entries.first().completedAt.toLocalDateTime(timeZone).date
        val count = (dateOccurrences[date] ?: 0) + 1
        dateOccurrences[date] = count
        if (count == 1) shortDate(date) else "${shortDate(date)} ($count)"
    }
    val weightBySession: List<Pair<String, List<Double>>> = sessionGroups.mapIndexed { i, (_, entries) ->
        sessionLabels[i] to entries.map { it.weight }
    }
    val repsBySession: List<Pair<String, Int>> = sessionGroups.mapIndexed { i, (_, entries) ->
        sessionLabels[i] to entries.sumOf { it.reps }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "weight_chart") {
            WeightProgressionChart(dataPoints = weightBySession)
        }
        item(key = "reps_chart") {
            RepsBarChart(dataPoints = repsBySession)
        }
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

private enum class WeightMode(val label: String) {
    Max("Max"),
    Average("Average"),
    FirstSet("First Set"),
}

@Composable
private fun WeightProgressionChart(dataPoints: List<Pair<String, List<Double>>>) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

    var mode by remember { mutableStateOf(WeightMode.Max) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val resolvedData: List<Pair<String, Double>> = dataPoints.map { (label, weights) ->
        label to when (mode) {
            WeightMode.Max -> weights.maxOrNull() ?: 0.0
            WeightMode.Average -> if (weights.isEmpty()) 0.0 else weights.average()
            WeightMode.FirstSet -> weights.firstOrNull() ?: 0.0
        }
    }

    val n = resolvedData.size
    val maxW = if (n > 0) resolvedData.maxOf { it.second } else 0.0
    val minW = if (n > 0) resolvedData.minOf { it.second } else 0.0
    val rangePad = if (maxW == minW) 10.0 else (maxW - minW) * 0.15
    val displayMin = (minW - rangePad).coerceAtLeast(0.0)
    val displayMax = maxW + rangePad
    val range = (displayMax - displayMin).coerceAtLeast(0.001)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Weight Progression", style = MaterialTheme.typography.titleSmall)
                Box {
                    TextButton(
                        onClick = { dropdownExpanded = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(mode.label, style = MaterialTheme.typography.labelMedium)
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        WeightMode.entries.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.label) },
                                onClick = { mode = m; dropdownExpanded = false },
                            )
                        }
                    }
                }
            }
            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val leftPad = 52.dp.toPx()
                val rightPad = 12.dp.toPx()
                val topPad = 4.dp.toPx()
                val bottomPad = 28.dp.toPx()
                val cw = size.width - leftPad - rightPad
                val ch = size.height - topPad - bottomPad

                // Y-axis gridlines and labels
                for (i in 0..3) {
                    val frac = i.toFloat() / 3f
                    val y = topPad + ch * (1f - frac)
                    drawLine(
                        color = outlineVariantColor.copy(alpha = 0.5f),
                        start = Offset(leftPad, y),
                        end = Offset(leftPad + cw, y),
                        strokeWidth = 1f,
                    )
                    val labelText = (displayMin + range * frac).roundToInt().toString()
                    val measured = textMeasurer.measure(labelText, TextStyle(fontSize = 9.sp))
                    drawText(
                        measured,
                        color = onSurfaceVariantColor,
                        topLeft = Offset(leftPad - measured.size.width - 4.dp.toPx(), y - measured.size.height / 2f),
                    )
                }

                // X positions — inset by hPad so the first/last point don't sit on the axis edge
                val hPad = 16.dp.toPx()
                val xs = if (n <= 1) {
                    FloatArray(n) { leftPad + cw / 2f }
                } else {
                    FloatArray(n) { i -> leftPad + hPad + i.toFloat() / (n - 1).toFloat() * (cw - 2 * hPad) }
                }

                // X-axis labels (up to 5)
                val labelStep = ((n.toFloat() / 5f).coerceAtLeast(1f)).toInt()
                for (i in 0 until n step labelStep) {
                    val measured = textMeasurer.measure(resolvedData[i].first, TextStyle(fontSize = 9.sp))
                    drawText(
                        measured,
                        color = onSurfaceVariantColor,
                        topLeft = Offset(xs[i] - measured.size.width / 2f, topPad + ch + 4.dp.toPx()),
                    )
                }

                // Line
                if (n > 1) {
                    val path = Path()
                    for (i in 0 until n) {
                        val frac = ((resolvedData[i].second - displayMin) / range).toFloat().coerceIn(0f, 1f)
                        val y = topPad + ch * (1f - frac)
                        if (i == 0) path.moveTo(xs[i], y) else path.lineTo(xs[i], y)
                    }
                    drawPath(path, primaryColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                }

                // Dots
                for (i in 0 until n) {
                    val frac = ((resolvedData[i].second - displayMin) / range).toFloat().coerceIn(0f, 1f)
                    val y = topPad + ch * (1f - frac)
                    drawCircle(primaryColor, radius = 4.dp.toPx(), center = Offset(xs[i], y))
                    drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(xs[i], y))
                }
            }
        }
    }
}

@Composable
private fun RepsBarChart(dataPoints: List<Pair<String, Int>>) {
    val textMeasurer = rememberTextMeasurer()
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

    val n = dataPoints.size
    val minReps = if (n > 0) dataPoints.minOf { it.second } else 0
    val maxReps = if (n > 0) dataPoints.maxOf { it.second } else 0
    val rangePad = if (maxReps == minReps) maxReps * 0.15f else (maxReps - minReps) * 0.15f
    val displayMin = (minReps - rangePad).coerceAtLeast(0f)
    val displayMax = (maxReps + rangePad).coerceAtLeast(1f)
    val range = (displayMax - displayMin).coerceAtLeast(0.001f)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reps per Session", style = MaterialTheme.typography.titleSmall)
            Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                val leftPad = 52.dp.toPx()
                val rightPad = 12.dp.toPx()
                val topPad = 4.dp.toPx()
                val bottomPad = 28.dp.toPx()
                val cw = size.width - leftPad - rightPad
                val ch = size.height - topPad - bottomPad

                // Y-axis gridlines and labels
                for (i in 0..3) {
                    val frac = i.toFloat() / 3f
                    val y = topPad + ch * (1f - frac)
                    drawLine(
                        color = outlineVariantColor.copy(alpha = 0.5f),
                        start = Offset(leftPad, y),
                        end = Offset(leftPad + cw, y),
                        strokeWidth = 1f,
                    )
                    val labelText = "${(displayMin + range * frac).toInt()}"
                    val measured = textMeasurer.measure(labelText, TextStyle(fontSize = 9.sp))
                    drawText(
                        measured,
                        color = onSurfaceVariantColor,
                        topLeft = Offset(leftPad - measured.size.width - 4.dp.toPx(), y - measured.size.height / 2f),
                    )
                }

                // Bars and X-axis labels
                val slotWidth = cw / n.coerceAtLeast(1)
                val barWidth = slotWidth * 0.6f
                val labelStep = ((n.toFloat() / 5f).coerceAtLeast(1f)).toInt()

                for (i in 0 until n) {
                    val cx = leftPad + i * slotWidth + slotWidth / 2f
                    val frac = ((dataPoints[i].second - displayMin) / range).coerceIn(0f, 1f)
                    val barHeight = (ch * frac).coerceAtLeast(2.dp.toPx())
                    drawRoundRect(
                        color = secondaryColor,
                        topLeft = Offset(cx - barWidth / 2f, topPad + ch - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                    if (i % labelStep == 0) {
                        val measured = textMeasurer.measure(dataPoints[i].first, TextStyle(fontSize = 9.sp))
                        drawText(
                            measured,
                            color = onSurfaceVariantColor,
                            topLeft = Offset(cx - measured.size.width / 2f, topPad + ch + 4.dp.toPx()),
                        )
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

private fun formatDate(date: LocalDate): String {
    val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$month ${date.day}, ${date.year}"
}

private fun shortDate(date: LocalDate): String {
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$month ${date.day}"
}
