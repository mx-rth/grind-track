package intellij.kmm.settings.grind_track.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import androidx.core.content.getSystemService
import intellij.kmm.settings.grind_track.core.database.entity.Routine
import java.util.Calendar
import kotlinx.datetime.DayOfWeek

internal const val ACTION_ROUTINE_REMINDER =
    "intellij.kmm.settings.grind_track.action.ROUTINE_REMINDER"
internal const val EXTRA_ROUTINE_ID = "routineId"
internal const val EXTRA_DAY_OF_WEEK = "dayOfWeek"

private const val PREFS_NAME = "routine_reminders"
private const val KEY_SCHEDULED = "scheduled_tuples"

actual class RoutineNotificationScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? = context.getSystemService<AlarmManager>()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    actual fun syncAll(routines: List<Routine>) {
        val manager = alarmManager ?: return
        ensureRoutineReminderChannel(context)

        // Cancel every alarm we've ever scheduled (across process boundaries).
        readScheduled().forEach { (id, day) -> cancelOne(manager, id, day) }

        val nextScheduled = mutableSetOf<Pair<Long, DayOfWeek>>()
        for (routine in routines) {
            val minute = routine.notificationMinuteOfDay
            if (!routine.notificationEnabled || minute == null || routine.scheduledDays.isEmpty()) {
                continue
            }
            for (day in routine.scheduledDays) {
                scheduleOne(manager, routine.id, day, minute)
                nextScheduled += routine.id to day
            }
        }
        writeScheduled(nextScheduled)
    }

    /** Re-arm a single (routineId, dayOfWeek) for the next future occurrence. */
    internal fun rearm(routineId: Long, day: DayOfWeek, minuteOfDay: Int) {
        val manager = alarmManager ?: return
        scheduleOne(manager, routineId, day, minuteOfDay)
        val updated = readScheduled() + (routineId to day)
        writeScheduled(updated)
    }

    private fun readScheduled(): Set<Pair<Long, DayOfWeek>> {
        val raw = prefs.getString(KEY_SCHEDULED, null) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { token ->
            val parts = token.split(":")
            val id = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val dayIso = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            val day = isoDayNumberToDayOfWeek(dayIso) ?: return@mapNotNull null
            id to day
        }.toSet()
    }

    private fun writeScheduled(set: Set<Pair<Long, DayOfWeek>>) {
        val raw = set.joinToString(",") { (id, day) -> "$id:${day.isoDayNumber}" }
        prefs.edit { putString(KEY_SCHEDULED, raw) }
    }

    private fun scheduleOne(manager: AlarmManager, routineId: Long, day: DayOfWeek, minuteOfDay: Int) {
        val triggerAt = nextOccurrenceMillis(day, minuteOfDay)
        val pi = createPendingIntent(routineId, day, flags = PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (canExact) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelOne(manager: AlarmManager, routineId: Long, day: DayOfWeek) {
        val pi = createPendingIntent(routineId, day, flags = PendingIntent.FLAG_NO_CREATE) ?: return
        manager.cancel(pi)
        pi.cancel()
    }

    private fun createPendingIntent(routineId: Long, day: DayOfWeek, flags: Int): PendingIntent? {
        val intent = Intent(context, RoutineReminderReceiver::class.java).apply {
            action = ACTION_ROUTINE_REMINDER
            putExtra(EXTRA_ROUTINE_ID, routineId)
            putExtra(EXTRA_DAY_OF_WEEK, day.isoDayNumber)
        }
        val baseFlags = PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context,
            requestCode(routineId, day),
            intent,
            flags or baseFlags,
        )
    }
}

internal fun requestCode(routineId: Long, day: DayOfWeek): Int =
    (routineId.hashCode() * 8) + day.isoDayNumber

internal val DayOfWeek.isoDayNumber: Int
    get() = when (this) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
    }

internal fun isoDayNumberToDayOfWeek(iso: Int): DayOfWeek? = when (iso) {
    1 -> DayOfWeek.MONDAY
    2 -> DayOfWeek.TUESDAY
    3 -> DayOfWeek.WEDNESDAY
    4 -> DayOfWeek.THURSDAY
    5 -> DayOfWeek.FRIDAY
    6 -> DayOfWeek.SATURDAY
    7 -> DayOfWeek.SUNDAY
    else -> null
}

/**
 * Compute the next-future epoch-millis at which [day] occurs at [minuteOfDay] local time.
 * If the requested moment is later today, returns today; otherwise the next matching weekday.
 */
internal fun nextOccurrenceMillis(day: DayOfWeek, minuteOfDay: Int, now: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    cal.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
    cal.set(Calendar.MINUTE, minuteOfDay % 60)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    // Calendar.MONDAY = 2 .. SUNDAY = 1; map ISO 1..7 to Calendar's value.
    val calTarget = if (day == DayOfWeek.SUNDAY) Calendar.SUNDAY else day.isoDayNumber + 1

    var deltaDays = (calTarget - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
    if (deltaDays == 0 && cal.timeInMillis <= now) deltaDays = 7
    cal.add(Calendar.DAY_OF_YEAR, deltaDays)
    return cal.timeInMillis
}
