package intellij.kmm.settings.grind_track.core.data

import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import intellij.kmm.settings.grind_track.core.database.entity.RoutineExercise

data class RoutineExerciseWithExercise(
    val routineExercise: RoutineExercise,
    val exercise: Exercise,
) {
    val effectiveRestSeconds: Int
        get() = routineExercise.restSecondsOverride ?: exercise.defaultRestSeconds
}
