package intellij.kmm.settings.grind_track.feature.settings.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import intellij.kmm.settings.grind_track.app.ThemeState
import intellij.kmm.settings.grind_track.core.notifications.CustomSound
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeOverride by ThemeState.isDark.collectAsStateWithLifecycle()
    val isDarkMode = themeOverride ?: isSystemInDarkTheme()
    val pickNotificationSound =
        rememberSoundFilePicker(onPicked = viewModel::installNotificationSound)
    val pickAlarmSound = rememberSoundFilePicker(onPicked = viewModel::installAlarmSound)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DarkModeSection(isDark = isDarkMode, onToggle = ThemeState::set)
            HorizontalDivider()
            SoundSection(
                sectionTitle = "Notification sound",
                description = "Plays when the rest timer ends.",
                currentSound = state.notificationSound,
                defaultLabel = "Default chime",
                onPick = pickNotificationSound,
                onReset = viewModel::resetNotificationSound,
                showThirtySecondHint = false,
            )
            SoundSection(
                sectionTitle = "Alarm sound",
                description = "Plays 15 seconds after the rest timer ends.",
                currentSound = state.alarmSound,
                defaultLabel = "Default alarm tone",
                onPick = pickAlarmSound,
                onReset = viewModel::resetAlarmSound,
                showThirtySecondHint = true,
            )
            Text(
                text = "iOS plays sounds up to 30 seconds. Files larger than that fall " +
                    "back to the default tone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DarkModeSection(isDark: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Dark mode", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (isDark) "Dark theme active" else "Light theme active",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = isDark, onCheckedChange = onToggle)
    }
}

@Composable
private fun SoundSection(
    sectionTitle: String,
    description: String,
    currentSound: CustomSound?,
    defaultLabel: String,
    onPick: () -> Unit,
    onReset: () -> Unit,
    showThirtySecondHint: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Current sound",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = currentSound?.displayName ?: defaultLabel,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Text("Pick a .wav file")
        }
        OutlinedButton(
            onClick = onReset,
            enabled = currentSound != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Reset to default")
        }
        if (showThirtySecondHint) {
            Text(
                text = "Tip: keep this clip under 30 seconds — iOS won't play longer files.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
