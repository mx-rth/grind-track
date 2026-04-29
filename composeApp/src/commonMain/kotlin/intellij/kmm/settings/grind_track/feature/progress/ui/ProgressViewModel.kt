package intellij.kmm.settings.grind_track.feature.progress.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import intellij.kmm.settings.grind_track.core.data.ProgressRepository
import intellij.kmm.settings.grind_track.core.database.dao.SetEntryRow
import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import intellij.kmm.settings.grind_track.core.database.entity.ExerciseType
import intellij.kmm.settings.grind_track.core.database.entity.SetEntry
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class AchievementStatus(
    val isObtained: Boolean,
    val obtainedDate: LocalDate? = null,
)

data class ProgressUiState(
    val exercises: List<Exercise> = emptyList(),
    val selectedExerciseId: Long? = null,
    val history: List<SetEntry> = emptyList(),
    val streak: Int = 0,
    val isLoading: Boolean = true,
    val achievementStatuses: Map<String, AchievementStatus> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    private val repository: ProgressRepository,
) : ViewModel() {

    private val explicitSelection = MutableStateFlow<Long?>(null)

    val state: StateFlow<ProgressUiState> = combine(
        combine(
            repository.observeExercisesWithActivity(),
            repository.observeStreak(),
            explicitSelection,
        ) { exercises, streak, explicit ->
            Triple(explicit ?: exercises.firstOrNull()?.id, exercises, streak)
        }.flatMapLatest { triple ->
            val effectiveId = triple.first
            val exercises = triple.second
            val streak = triple.third
            if (effectiveId == null) {
                flowOf(ProgressUiState(exercises = exercises, streak = streak, isLoading = false))
            } else {
                repository.observeHistoryForExercise(effectiveId).map { history ->
                    ProgressUiState(
                        exercises = exercises,
                        selectedExerciseId = effectiveId,
                        history = history,
                        streak = streak,
                        isLoading = false,
                    )
                }
            }
        },
        repository.observeAllWithExerciseType(),
    ) { baseState, allRows ->
        baseState.copy(achievementStatuses = computeAchievements(baseState.streak, allRows))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun selectExercise(exerciseId: Long) {
        explicitSelection.value = exerciseId
    }
}

private fun computeAchievements(streak: Int, rows: List<SetEntryRow>): Map<String, AchievementStatus> {
    val tz = TimeZone.currentSystemDefault()

    // Group all rows by exercise, sorted chronologically within each group
    val byExercise: Map<Long, List<SetEntryRow>> = rows.groupBy { it.exerciseId }

    // --- Boot Sequence: streak >= 7 ---
    val bootSequence = AchievementStatus(isObtained = streak >= 7)

    // --- Cyberpsycho: streak >= 30 ---
    val cyberpsycho = AchievementStatus(isObtained = streak >= 30)

    // --- Overclocked: any STRENGTH exercise where a later session's max weight is >= 120% of first session ---
    var overclockedDate: LocalDate? = null
    outer@ for ((_, entries) in byExercise) {
        if (entries.firstOrNull()?.exerciseType != ExerciseType.STRENGTH) continue
        val sessions = entries
            .groupBy { it.sessionId }
            .entries.sortedBy { (_, v) -> v.first().completedAt }
        if (sessions.size < 2) continue
        val firstMax = sessions.first().value.maxOf { it.weight }
        if (firstMax <= 0) continue
        val threshold = firstMax * 1.2
        for ((_, sessionEntries) in sessions.drop(1)) {
            if (sessionEntries.maxOf { it.weight } >= threshold) {
                overclockedDate = sessionEntries.maxOf { it.completedAt }.toLocalDateTime(tz).date
                break@outer
            }
        }
    }
    val overclocked = AchievementStatus(isObtained = overclockedDate != null, obtainedDate = overclockedDate)

    // --- Peaked: any STRENGTH exercise where last session's max weight is <= 80% of first session ---
    var peakedDate: LocalDate? = null
    for ((_, entries) in byExercise) {
        if (entries.firstOrNull()?.exerciseType != ExerciseType.STRENGTH) continue
        val sessions = entries
            .groupBy { it.sessionId }
            .entries.sortedBy { (_, v) -> v.first().completedAt }
        if (sessions.size < 2) continue
        val firstMax = sessions.first().value.maxOf { it.weight }
        if (firstMax <= 0) continue
        val lastSession = sessions.last()
        if (lastSession.value.maxOf { it.weight } <= firstMax * 0.8) {
            peakedDate = lastSession.value.maxOf { it.completedAt }.toLocalDateTime(tz).date
            break
        }
    }
    val peaked = AchievementStatus(isObtained = peakedDate != null, obtainedDate = peakedDate)

    // --- David Martinez: any TIME exercise where a later session's total duration is <= 80% of first session ---
    var davidDate: LocalDate? = null
    outer@ for ((_, entries) in byExercise) {
        if (entries.firstOrNull()?.exerciseType != ExerciseType.TIME) continue
        val sessions = entries
            .groupBy { it.sessionId }
            .entries.sortedBy { (_, v) -> v.first().completedAt }
        if (sessions.size < 2) continue
        val firstDuration = sessions.first().value.mapNotNull { it.durationSeconds }.sum()
        if (firstDuration <= 0) continue
        val threshold = firstDuration * 0.8
        for ((_, sessionEntries) in sessions.drop(1)) {
            val duration = sessionEntries.mapNotNull { it.durationSeconds }.sum()
            if (duration > 0 && duration <= threshold) {
                davidDate = sessionEntries.maxOf { it.completedAt }.toLocalDateTime(tz).date
                break@outer
            }
        }
    }
    val david = AchievementStatus(isObtained = davidDate != null, obtainedDate = davidDate)

    // --- Extra Mile: any DISTANCE exercise where a later session's total distance is >= 120% of first session ---
    var extraMileDate: LocalDate? = null
    outer@ for ((_, entries) in byExercise) {
        if (entries.firstOrNull()?.exerciseType != ExerciseType.DISTANCE) continue
        val sessions = entries
            .groupBy { it.sessionId }
            .entries.sortedBy { (_, v) -> v.first().completedAt }
        if (sessions.size < 2) continue
        val firstDistance = sessions.first().value.mapNotNull { it.distanceMeters }.sum()
        if (firstDistance <= 0) continue
        val threshold = firstDistance * 1.2
        for ((_, sessionEntries) in sessions.drop(1)) {
            val distance = sessionEntries.mapNotNull { it.distanceMeters }.sum()
            if (distance >= threshold) {
                extraMileDate = sessionEntries.maxOf { it.completedAt }.toLocalDateTime(tz).date
                break@outer
            }
        }
    }
    val extraMile = AchievementStatus(isObtained = extraMileDate != null, obtainedDate = extraMileDate)

    return mapOf(
        "7day_streak" to bootSequence,
        "30day_streak" to cyberpsycho,
        "strength" to overclocked,
        "consistency" to peaked,
        "speed" to david,
        "milestone" to extraMile,
    )
}
