package intellij.kmm.settings.grind_track.core.data

import intellij.kmm.settings.grind_track.core.database.dao.SetEntryDao
import intellij.kmm.settings.grind_track.core.database.entity.Exercise
import intellij.kmm.settings.grind_track.core.database.entity.SetEntry
import kotlinx.coroutines.flow.Flow

class ProgressRepository(
    private val setEntryDao: SetEntryDao,
) {
    fun observeExercisesWithActivity(): Flow<List<Exercise>> =
        setEntryDao.observeExercisesWithActivity()

    fun observeHistoryForExercise(exerciseId: Long): Flow<List<SetEntry>> =
        setEntryDao.observeHistoryForExercise(exerciseId)
}
