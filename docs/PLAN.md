# grind-track — Product Plan

## Product description

grind-track is a Kotlin Multiplatform (Android + iOS) gym workout tracker. The app supports four core flows:

1. **Build a routine.** Create a workout routine as an ordered sequence of exercises. Each entry specifies a target number of sets, target reps (or "to failure"), and an optional per-exercise rest override.
2. **Replay it.** Pick a routine and step through it set by set during a workout session.
3. **Log sets.** When a set is marked complete, a per-exercise rest timer counts down; during the rest the user enters the actual weight and reps performed, persisted as a `SetEntry`.
4. **Track progress.** A progress screen reads the persisted history and visualises per-exercise improvement over time.

The UI is a three-tab bottom-navigation app: **Routines**, **Workout**, **Progress**.

## Current implementation (2026-04-28)

All four milestones of the original plan are implemented; `./gradlew :composeApp:check` is green for Android (debug + release) and `iosSimulatorArm64`. What ships today:

### Routines
- List of routines with a "+" FAB. Tapping the FAB inserts a "New routine" row and navigates to its editor; per-row delete in place.
- Editor: editable name, ordered list of routine-exercises, each with inline `Sets` / `Reps` / `Rest` fields and up/down/remove controls. A "To failure (max reps)" checkbox swaps the reps field for a `null` target — the actual reps performed are captured at workout time.
- Add-exercise dialog: pick from the catalogue or create a new exercise inline. The create flow uses a single combined "Create & add" form so rest seconds are entered exactly once.

### Workout session
- The active session is observed from the DB so the screen flips automatically between picker and in-session as sessions start/finish.
- Picker: list of routines as cards. Tap to start. Empty-state copy when no routines exist.
- In-session: routine name in the top bar, current exercise headline, `Set X of Y`, the target reps (`Target: 8 reps` or `Reps: to failure`), and the upcoming rest interval.
- "Mark set complete" enters the rest phase: a coroutine-driven 1 Hz countdown plus a `Weight` + `Reps` form prefilled from the last logged set of that exercise (within the session). "Log set & continue" persists a `SetEntry` and advances the state machine; "Skip logging" advances without writing. Timer is cancelled on every advance, on Finish, and in `onCleared`.
- A platform-level **rest-end alarm** fires alongside the in-app countdown so the user is woken even with the app backgrounded or the screen off. KMP `expect class RestTimerAlarm` with platform actuals: Android uses `AlarmManager.setExactAndAllowWhileIdle` → `RestTimerReceiver` → high-importance notification on the `rest_timer_alarm` channel with `RingtoneManager.TYPE_ALARM` sound and vibration (the system plays the alarm tone continuously until the notification is dismissed). iOS schedules a sequence of `UNTimeIntervalNotificationTrigger` notifications spaced 4 s apart (six total, ~24 s of repeated dings) sharing a `threadIdentifier` so they group on the lock screen — iOS has no API for a single long alarm tone outside the Critical Alerts entitlement, and bundling a custom ≤30 s sound is a separate follow-up. A foreground delegate makes the sound play even when the app is open. Cancellation on advance/finish removes all pending and delivered notifications.
- Routine deletion mid-session (FK `SET_NULL` on `routineId`) auto-finishes the session.

### Progress
- A `LazyRow` of `FilterChip`s lists every exercise that has at least one logged set, ordered most-recent first; the most-recent exercise is selected by default and the user can switch.
- Per-day grouped history list. Each day is an `ElevatedCard` with a `Month D, YYYY` heading, then rows like `Set N    W × R`.

### Persistence
- Room v3, `BundledSQLiteDriver`, schemas exported to `composeApp/schemas`. Five entities: `Exercise`, `Routine`, `RoutineExercise` (with `targetReps: Int?` — null means "to failure"), `WorkoutSession`, `SetEntry`.
- Alpha-stage destructive migration via `fallbackToDestructiveMigration(dropAllTables = true)` in the DI builder. There is no real migration code; bumping `version` wipes data.
- Repositories in `core/data/`: `RoutineRepository`, `ExerciseRepository`, `WorkoutRepository`, `ProgressRepository`. All Koin singletons.

### UI architecture
- One ViewModel per feature screen via `koin-compose-viewmodel`: `RoutinesViewModel`, `RoutineEditorViewModel(routineId)` (parameterised), `WorkoutViewModel`, `ProgressViewModel`.
- Each ViewModel exposes a `StateFlow<UiState>`. Screens are stateless except for transient form drafts hoisted close to the inputs.
- Navigation: top-level destinations are tabs; the only deep route is `routine/{routineId}` for the editor (`NavType.LongType`, args read via `androidx.savedstate.read { getLong(...) }`).

## Outstanding items / known gaps

- **Position persistence on cold start.** Mid-session app kill currently restarts at exercise 1 / set 1 even though logged `SetEntry` rows persist. A small follow-up: derive the next position from per-`routine_exercise` `SetEntry` counts when the active session is observed.
- **Chart on the progress screen.** The original plan called for a Compose canvas line chart of top-set weight / e1RM. Only the tabular history is built; the chart is unbuilt.
- **Tests.** No ViewModel or DAO tests yet. The convention ("one happy-path + one error-path per ViewModel") would naturally use turbine + koin-test fakes for ViewModels and Room's in-memory builder + `BundledSQLiteDriver` for DAOs.
- **Deprecation cleanup.** A handful of pre-existing warnings: `Icons.Filled.{ArrowBack, List}` are deprecated in favour of `Icons.AutoMirrored.Filled.*`; entities and `InstantTypeConverter` still reference the deprecated `kotlinx.datetime.Instant` typealias instead of `kotlin.time.Instant`.
- **Material icons set.** The project pulls `material-icons-core` only. Adding a feature that needs an icon outside the core set (`Delete`, `Save`, …) means either adding `material-icons-extended` as a dependency or substituting from core (`Close`, `Check`) like the current code does.
- **Custom alarm melody.** Android plays the OS default alarm tone continuously via `RingtoneManager.TYPE_ALARM` on the `USAGE_ALARM` audio stream. iOS approximates a long alarm by scheduling six default-sound notifications 4 s apart (~24 s of repeated dings). Replacing this with a single ≤30 s custom alarm sound on iOS would require: ship a `caf`/`aiff`/`wav` sound file in the iOS app bundle (added to `iosApp/iosApp/` and registered as a resource in `iosApp.xcodeproj`), then change `RestTimerAlarm.ios.kt` to schedule a single notification with `UNNotificationSound.soundNamed("alarm.caf")` instead of the repeat sequence. Critical alerts on iOS (sound on silent / DND) require an Apple-approved entitlement and are not in scope.
- **No real Room migrations.** Acceptable while the app is alpha and there's no production data; before any external release this will need actual `Migration` objects.

## Convention reminders for future work

- New features go in `feature/<name>/{ui[, data, domain]}`. ViewModels via `koin-compose-viewmodel`, repositories via Koin singletons.
- Verify with `./gradlew :composeApp:check --no-daemon` (preapproved in `.claude/settings.local.json`). The JBR 21 `JAVA_HOME` workaround is in the same allowlist if Gradle ever fails on JDK version.
- Reach for `kotlin.time.Clock` / `kotlin.time.Instant` in new code rather than the deprecated `kotlinx.datetime` aliases.
