package intellij.kmm.settings.grind_track.feature.routines.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import intellij.kmm.settings.grind_track.core.designsystem.AccentBadge
import intellij.kmm.settings.grind_track.core.designsystem.BrandColors
import intellij.kmm.settings.grind_track.core.designsystem.EmptyState
import intellij.kmm.settings.grind_track.core.designsystem.HeroCard
import intellij.kmm.settings.grind_track.core.designsystem.SectionHeader
import intellij.kmm.settings.grind_track.core.designsystem.StripedCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    onOpenRoutine: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: RoutinesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val newRoutineId by viewModel.newRoutineId.collectAsStateWithLifecycle()

    LaunchedEffect(newRoutineId) {
        newRoutineId?.let {
            onOpenRoutine(it)
            viewModel.consumeNewRoutineId()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Routines", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::createRoutine,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  New routine")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.routines.isEmpty() && !state.isLoading) {
                EmptyState(
                    title = "No routines yet",
                    subtitle = "Tap + to create your first routine.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "hero") {
                        HeroCard(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            onColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            AccentBadge(
                                text = "Today",
                                container = MaterialTheme.colorScheme.onPrimaryContainer,
                                onContainer = MaterialTheme.colorScheme.primaryContainer,
                            )
                            Text(
                                text = "Let's crush\nyour next set.",
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            Text(
                                text = "${state.routines.size} routine${if (state.routines.size == 1) "" else "s"} ready to go",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    item(key = "section") {
                        SectionHeader(text = "Your routines")
                    }
                    items(state.routines, key = { it.id }) { routine ->
                        RoutineRow(
                            routine = routine,
                            onClick = { onOpenRoutine(routine.id) },
                            onDelete = { viewModel.deleteRoutine(routine.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineRow(
    routine: Routine,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = accentForRoutine(routine.id)
    StripedCard(accent = accent, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(end = 12.dp),
            ) {
                Box(modifier = Modifier.padding(10.dp)) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name.ifBlank { "Untitled routine" },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Delete routine")
            }
        }
    }
}

private fun accentForRoutine(id: Long): androidx.compose.ui.graphics.Color {
    val palette = listOf(
        BrandColors.Coral,
        BrandColors.Electric,
        BrandColors.SunYellow,
        BrandColors.MintFresh,
    )
    val idx = ((id % palette.size).toInt() + palette.size) % palette.size
    return palette[idx]
}
