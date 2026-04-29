package intellij.kmm.settings.grind_track.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import intellij.kmm.settings.grind_track.core.data.ProgressRepository
import intellij.kmm.settings.grind_track.feature.progress.ui.CelebrationEvent
import intellij.kmm.settings.grind_track.feature.progress.ui.allAchievements
import intellij.kmm.settings.grind_track.feature.progress.ui.computeAchievements
import intellij.kmm.settings.grind_track.feature.progress.ui.streakMilestones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CelebrationViewModel(private val repository: ProgressRepository) : ViewModel() {

    private val _queue = MutableStateFlow<List<CelebrationEvent>>(emptyList())

    val currentEvent: StateFlow<CelebrationEvent?> = _queue
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun dismiss() {
        _queue.update { it.drop(1) }
    }

    private var knownObtained: Set<String>? = null
    private var knownStreak: Int? = null

    init {
        viewModelScope.launch {
            combine(
                repository.observeStreak(),
                repository.observeAllWithExerciseType(),
            ) { streak, allRows ->
                streak to computeAchievements(streak, allRows)
            }.collect { (streak, statuses) ->
                checkAchievements(statuses)
                checkStreak(streak)
            }
        }
    }

    private fun checkAchievements(statuses: Map<String, intellij.kmm.settings.grind_track.feature.progress.ui.AchievementStatus>) {
        val currentlyObtained = statuses.filter { (_, v) -> v.isObtained }.keys.toSet()
        val existing = knownObtained
        if (existing == null) {
            knownObtained = currentlyObtained
            return
        }
        val newlyUnlocked = currentlyObtained - existing
        newlyUnlocked.forEach { id ->
            allAchievements.firstOrNull { it.id == id }?.let { achievement ->
                _queue.update { queue ->
                    queue + CelebrationEvent(
                        title = "Achievement Unlocked!",
                        subtitle = achievement.name,
                        imageRes = achievement.imageRes,
                    )
                }
            }
        }
        knownObtained = currentlyObtained
    }

    private fun checkStreak(streak: Int) {
        val prev = knownStreak
        if (prev == null) {
            knownStreak = streak
            return
        }
        if (streak > prev) {
            val crossed = streakMilestones.firstOrNull { it in (prev + 1)..streak }
            if (crossed != null) {
                val (title, subtitle) = if (crossed == 1) {
                    "Streak Started!" to "Day 1 — keep the momentum going!"
                } else {
                    "$crossed Day Streak!" to "You're on fire — keep going!"
                }
                _queue.update { it + CelebrationEvent(title = title, subtitle = subtitle) }
            }
        }
        knownStreak = streak
    }
}
