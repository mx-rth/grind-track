package intellij.kmm.settings.grind_track.core.data

import intellij.kmm.settings.grind_track.core.database.dao.ExerciseDao
import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import intellij.kmm.settings.grind_track.core.database.entity.ExerciseType
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(
    private val dao: ExerciseDao,
) {
    fun observeExercises(): Flow<List<Exercise>> = dao.observeAll()

    suspend fun create(
        name: String,
        defaultRestSeconds: Int,
        defaultRestBetweenExercisesSeconds: Int,
        unilateral: Boolean = false,
        defaultRestAfterFirstSideSeconds: Int = 60,
        bodyWeight: Boolean = false,
        type: ExerciseType = ExerciseType.STRENGTH,
    ): Long = dao.insert(
        Exercise(
            name = name.trim(),
            defaultRestSeconds = defaultRestSeconds,
            defaultRestBetweenExercisesSeconds = defaultRestBetweenExercisesSeconds,
            unilateral = unilateral,
            defaultRestAfterFirstSideSeconds = defaultRestAfterFirstSideSeconds,
            bodyWeight = bodyWeight,
            type = type,
        )
    )

    suspend fun update(exercise: Exercise) = dao.update(exercise)

    suspend fun delete(exercise: Exercise) = dao.delete(exercise)
}
