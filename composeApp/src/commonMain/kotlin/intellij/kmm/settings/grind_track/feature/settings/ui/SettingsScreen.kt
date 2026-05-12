package intellij.kmm.settings.grind_track.feature.settings.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import intellij.kmm.settings.grind_track.app.ThemeState
import intellij.kmm.settings.grind_track.core.designsystem.BrandColors
import intellij.kmm.settings.grind_track.core.designsystem.MascotPose
import intellij.kmm.settings.grind_track.core.designsystem.MascotVariant
import intellij.kmm.settings.grind_track.core.designsystem.mascotResource
import intellij.kmm.settings.grind_track.core.notifications.CustomSound
import org.jetbrains.compose.resources.painterResource
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
            MascotPicker(
                selected = state.mascotVariant,
                onSelect = viewModel::selectMascot,
            )
            DarkModeSection(isDark = isDarkMode, onToggle = ThemeState::set)
            HorizontalDivider()
            SoundSection(
                sectionTitle = "Notification sound",
                description = "Plays when the rest timer ends.",
                currentSound = state.notificationSound,
                defaultLabel = "Default chime",
                onPick = pickNotificationSound,
                onReset = viewModel::resetNotificationSound,
                gainDb = state.notificationGainDb,
                onGainChange = viewModel::setNotificationGainDb,
                showThirtySecondHint = false,
            )
            SoundSection(
                sectionTitle = "Alarm sound",
                description = "Plays 15 seconds after the rest timer ends.",
                currentSound = state.alarmSound,
                defaultLabel = "Default alarm tone",
                onPick = pickAlarmSound,
                onReset = viewModel::resetAlarmSound,
                gainDb = state.alarmGainDb,
                onGainChange = viewModel::setAlarmGainDb,
                showThirtySecondHint = true,
            )
            Text(
                text = "iOS plays sounds up to 30 seconds. Files larger than that fall " +
                    "back to the default tone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            DeveloperSection(onAddMockData = viewModel::seedMockData)
        }
    }
}

@Composable
private fun DeveloperSection(onAddMockData: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Developer",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Seed the database with sample routines and 6 weeks of past " +
                "workouts. No-op if data already exists.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onAddMockData, modifier = Modifier.fillMaxWidth()) {
            Text("Add mock data")
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
private fun MascotPicker(
    selected: MascotVariant,
    onSelect: (MascotVariant) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Workout buddy",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Pick the mascot that cheers you on across the app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MascotVariant.entries.forEach { variant ->
                MascotOption(
                    variant = variant,
                    selected = variant == selected,
                    onClick = { onSelect(variant) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MascotOption(
    variant: MascotVariant,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (variant == MascotVariant.Female) BrandColors.Cyan else BrandColors.SunYellow
    val container = if (selected) accent else MaterialTheme.colorScheme.surface
    val onContainer = if (selected) BrandColors.InkNavy else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = onContainer,
        tonalElevation = if (selected) 0.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                Image(
                    painter = painterResource(mascotResource(variant, MascotPose.Standing)),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
            }
            Text(
                text = if (variant == MascotVariant.Female) "Cy" else "Mo",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (selected) "Active" else "Tap to choose",
                style = MaterialTheme.typography.labelSmall,
                color = onContainer.copy(alpha = 0.7f),
            )
        }
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
    gainDb: Float,
    onGainChange: (Float) -> Unit,
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
        if (currentSound != null) {
            VolumeBoostControl(gainDb = gainDb, onGainChange = onGainChange)
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

@Composable
private fun VolumeBoostControl(
    gainDb: Float,
    onGainChange: (Float) -> Unit,
) {
    val rounded = gainDb.roundToInt()
    val valueLabel = if (rounded == 0) "Normal (auto-normalized)" else "+$rounded dB"
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Volume boost",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = gainDb,
            onValueChange = onGainChange,
            valueRange = 0f..12f,
            steps = 11,
        )
        Text(
            text = "Imported sounds are auto-normalized. Use this to push further.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
