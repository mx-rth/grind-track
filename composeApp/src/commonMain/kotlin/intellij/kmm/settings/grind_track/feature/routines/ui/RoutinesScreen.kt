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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import intellij.kmm.settings.grind_track.core.designsystem.EmptyState
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
        topBar = {
            TopAppBar(
                title = { Text("Routines") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::createRoutine) {
                Icon(Icons.Filled.Add, contentDescription = "New routine")
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineRow(
    routine: Routine,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = routine.name.ifBlank { "Untitled routine" },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Delete routine")
            }
        }
    }
}
