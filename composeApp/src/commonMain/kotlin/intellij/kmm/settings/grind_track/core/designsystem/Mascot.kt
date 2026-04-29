package intellij.kmm.settings.grind_track.core.designsystem

import grind_track.composeapp.generated.resources.Res
import grind_track.composeapp.generated.resources.mascot_f_check_progress
import grind_track.composeapp.generated.resources.mascot_f_clapping
import grind_track.composeapp.generated.resources.mascot_f_hands_on_hips
import grind_track.composeapp.generated.resources.mascot_f_high_five
import grind_track.composeapp.generated.resources.mascot_f_lets_go
import grind_track.composeapp.generated.resources.mascot_f_pointing
import grind_track.composeapp.generated.resources.mascot_f_ready_to_train
import grind_track.composeapp.generated.resources.mascot_f_standing
import grind_track.composeapp.generated.resources.mascot_f_thumbs_up
import grind_track.composeapp.generated.resources.mascot_f_wave
import grind_track.composeapp.generated.resources.mascot_m_check_progress
import grind_track.composeapp.generated.resources.mascot_m_clapping
import grind_track.composeapp.generated.resources.mascot_m_hands_on_hips
import grind_track.composeapp.generated.resources.mascot_m_high_five
import grind_track.composeapp.generated.resources.mascot_m_lets_go
import grind_track.composeapp.generated.resources.mascot_m_pointing
import grind_track.composeapp.generated.resources.mascot_m_ready_to_train
import grind_track.composeapp.generated.resources.mascot_m_standing
import grind_track.composeapp.generated.resources.mascot_m_thumbs_up
import grind_track.composeapp.generated.resources.mascot_m_wave
import org.jetbrains.compose.resources.DrawableResource

/** The two mascot variants ship with the same 10 poses. */
enum class MascotVariant { Female, Male }

/**
 * Mascot poses, useful for celebrating workout state in the UI. Each one is shipped as a
 * trimmed PNG with a transparent background.
 */
enum class MascotPose {
    Standing,
    ThumbsUp,
    Clapping,
    Pointing,
    LetsGo,
    CheckProgress,
    ReadyToTrain,
    HighFive,
    HandsOnHips,
    Wave,
}

/** Resolve the drawable resource for a given mascot variant + pose. */
fun mascotResource(variant: MascotVariant, pose: MascotPose): DrawableResource = when (variant) {
    MascotVariant.Female -> when (pose) {
        MascotPose.Standing -> Res.drawable.mascot_f_standing
        MascotPose.ThumbsUp -> Res.drawable.mascot_f_thumbs_up
        MascotPose.Clapping -> Res.drawable.mascot_f_clapping
        MascotPose.Pointing -> Res.drawable.mascot_f_pointing
        MascotPose.LetsGo -> Res.drawable.mascot_f_lets_go
        MascotPose.CheckProgress -> Res.drawable.mascot_f_check_progress
        MascotPose.ReadyToTrain -> Res.drawable.mascot_f_ready_to_train
        MascotPose.HighFive -> Res.drawable.mascot_f_high_five
        MascotPose.HandsOnHips -> Res.drawable.mascot_f_hands_on_hips
        MascotPose.Wave -> Res.drawable.mascot_f_wave
    }
    MascotVariant.Male -> when (pose) {
        MascotPose.Standing -> Res.drawable.mascot_m_standing
        MascotPose.ThumbsUp -> Res.drawable.mascot_m_thumbs_up
        MascotPose.Clapping -> Res.drawable.mascot_m_clapping
        MascotPose.Pointing -> Res.drawable.mascot_m_pointing
        MascotPose.LetsGo -> Res.drawable.mascot_m_lets_go
        MascotPose.CheckProgress -> Res.drawable.mascot_m_check_progress
        MascotPose.ReadyToTrain -> Res.drawable.mascot_m_ready_to_train
        MascotPose.HighFive -> Res.drawable.mascot_m_high_five
        MascotPose.HandsOnHips -> Res.drawable.mascot_m_hands_on_hips
        MascotPose.Wave -> Res.drawable.mascot_m_wave
    }
}
