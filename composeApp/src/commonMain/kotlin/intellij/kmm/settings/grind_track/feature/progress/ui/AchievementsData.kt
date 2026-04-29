package intellij.kmm.settings.grind_track.feature.progress.ui

import grind_track.composeapp.generated.resources.Res
import grind_track.composeapp.generated.resources.achievement_30day_streak
import grind_track.composeapp.generated.resources.achievement_7day_streak
import grind_track.composeapp.generated.resources.achievement_consistency
import grind_track.composeapp.generated.resources.achievement_milestone
import grind_track.composeapp.generated.resources.achievement_speed
import grind_track.composeapp.generated.resources.achievement_strength
import org.jetbrains.compose.resources.DrawableResource

data class Achievement(
    val id: String,
    val name: String,
    val howToObtain: String,
    val imageRes: DrawableResource,
)

data class CelebrationEvent(
    val title: String,
    val subtitle: String,
    val imageRes: DrawableResource? = null, // null → flame icon (streak)
)

val streakMilestones = listOf(1, 7, 14, 21, 30, 60, 90, 180, 365)

val allAchievements = listOf(
    Achievement(
        id = "7day_streak",
        name = "Boot Sequence",
        howToObtain = "Reach a 7-day streak by completing 7 consecutive planned workouts.",
        imageRes = Res.drawable.achievement_7day_streak,
    ),
    Achievement(
        id = "30day_streak",
        name = "Cyberpsycho",
        howToObtain = "Reach a 30-day streak by completing 30 consecutive planned workouts.",
        imageRes = Res.drawable.achievement_30day_streak,
    ),
    Achievement(
        id = "strength",
        name = "Overclocked",
        howToObtain = "Increase your max weight on any exercise by 20% compared to your first logged session.",
        imageRes = Res.drawable.achievement_strength,
    ),
    Achievement(
        id = "consistency",
        name = "Peaked",
        howToObtain = "Decrease your max weight on any exercise by 20% compared to your last logged session.",
        imageRes = Res.drawable.achievement_consistency,
    ),
    Achievement(
        id = "speed",
        name = "David Martinez",
        howToObtain = "For a time-based exercise, complete it 20% faster than your first logged session.",
        imageRes = Res.drawable.achievement_speed,
    ),
    Achievement(
        id = "milestone",
        name = "Extra Mile",
        howToObtain = "For a distance-based exercise, log 20% more distance than your first logged session.",
        imageRes = Res.drawable.achievement_milestone,
    ),
)
