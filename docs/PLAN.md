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
- The platform-level **rest-end alert** fires in two stages so the user is reliably woken even with the app backgrounded or the screen off. Both stages live in `RestTimerAlarm`. Cancellation on advance/finish removes both stages.
  - **Stage 1 — gentle chime at T+seconds.** Android: `AlarmManager.setExactAndAllowWhileIdle` → `RestTimerReceiver` (`ACTION_REST_DONE`) posts on the `rest_timer_complete` channel (default importance, default notification sound, plays once); when a custom notification .wav is installed it instead posts on the `rest_timer_complete_custom_$gen` channel (notification volume, user's sound). iOS: `UNTimeIntervalNotificationTrigger` with `UNNotificationSound.soundNamed(customFilename)` when a custom notification sound is installed, otherwise `UNNotificationSound.defaultSound`.
  - **Stage 2 — alarm at T+seconds+15.** Android: a second `setExactAndAllowWhileIdle` (`ACTION_REST_FOLLOWUP_DONE`, distinct request code `0x6712`). The default path posts on the alarm-style channel `rest_timer_alarm` (`RingtoneManager.TYPE_ALARM` + `USAGE_ALARM` — system loops the alarm tone until dismissed). The custom-sound path is different: the receiver delegates to `RestAlarmPlaybackService` (foreground service, `shortService` type) which plays the user's .wav with `MediaPlayer.setLooping(true)` on the `USAGE_ALARM` stream and posts its own foreground notification on the (silent) `rest_timer_alarm_custom_$gen` channel. The service self-stops after 60 s or when the user swipes the notification away (via its `setDeleteIntent`). iOS: a second `UNTimeIntervalNotificationTrigger` with `interruptionLevel = .timeSensitive` (breaks through Focus modes); uses `UNNotificationSound.soundNamed(customFilename)` when a custom alarm sound is installed, otherwise `UNNotificationSound.defaultSound`. Time-sensitive does NOT bypass the physical mute switch — that's gated behind Critical Alerts, which requires Apple approval. The Time Sensitive entitlement is declared in `iosApp/iosApp/iOSApp.entitlements` (`com.apple.developer.usernotifications.time-sensitive`) and wired via `CODE_SIGN_ENTITLEMENTS` in the pbxproj.
  - A foreground delegate makes both sounds play even when the app is open.
- **Inter-exercise rest timer.** After the last set of an exercise (when there is a next exercise queued), the session enters `Phase.RestingBeforeNextExercise`: a separate countdown showing "Up next: <exercise>" and reusing the same OS alarm slot. Duration is configured per-exercise via `Exercise.defaultRestBetweenExercisesSeconds` (default 180s) with an optional `RoutineExercise.restBetweenExercisesOverride`. An override of `0` opts the rest out entirely.
- Routine deletion mid-session (FK `SET_NULL` on `routineId`) auto-finishes the session.

### Progress
- A `LazyRow` of `FilterChip`s lists every exercise that has at least one logged set, ordered most-recent first; the most-recent exercise is selected by default and the user can switch.
- Per-day grouped history list. Each day is an `ElevatedCard` with a `Month D, YYYY` heading, then rows like `Set N    W × R`.

### Routine reminders
- Optional per-routine notification reminder. Two new fields on `Routine`: `notificationEnabled: Boolean` and `notificationMinuteOfDay: Int?` (minutes since midnight). The reminder fires only on the routine's `scheduledDays`.
- Wired into the existing routine editor: a `Switch` "Reminder notification" + a `TimePicker` dialog (material3) for choosing the time. Helper text prompts the user to pick training days when none are set.
- A common `RoutineNotificationsCoordinator` started in `initKoin` collects `RoutineRepository.observeRoutines()` and re-syncs the platform `RoutineNotificationScheduler` on every emit. This keeps notifications reactive to renames, schedule changes, time changes, toggle changes, and deletions without scattering scheduling calls.
- **Android**: one `AlarmManager.setExactAndAllowWhileIdle` per (routineId, dayOfWeek). Re-armed in `RoutineReminderReceiver` after each fire (next-week occurrence). The set of currently-scheduled (id, day) tuples is persisted in `SharedPreferences` so orphans (notifications scheduled before a process death where the routine has since been disabled/deleted) get cancelled on the next sync. New `routine_reminders` channel (default importance, default sound). `RoutineReminderBootReceiver` re-arms after `BOOT_COMPLETED` / `TIMEZONE_CHANGED`. New `RECEIVE_BOOT_COMPLETED` permission.
- **iOS**: one `UNCalendarNotificationTrigger` per (routineId, dayOfWeek) with `repeats = true`. Identifiers are prefixed `routine_<id>_day_<ordinal>`. iOS handles reboot/timezone natively. Authorization is shared with the rest-timer alarm.

### Settings (custom notification sounds)
- Reachable via a gear `IconButton` in the Routines tab top bar (no new bottom-nav tab). The screen has **two** independent sections — "Notification sound" (stage-1 chime) and "Alarm sound" (stage-2 alarm) — each with its own current-sound display, "Pick a .wav file" picker, and "Reset to default" button. The 30-second iOS cap is surfaced inline next to the alarm section and globally at the bottom of the screen.
- Both sounds are managed by the same `expect class CustomSoundManager`, parameterised by a `CustomSoundKind` (`Notification` | `Alarm`). Two singletons are registered in Koin under named qualifiers `custom_sound_notification` and `custom_sound_alarm` and consumed by both `RestTimerAlarm` (one per stage) and `SettingsViewModel`. The Android channel naming differs by kind (`rest_timer_complete_custom_$gen` for Notification with `USAGE_NOTIFICATION` + `IMPORTANCE_DEFAULT`; `rest_timer_alarm_custom_$gen` for Alarm with `USAGE_ALARM` + `IMPORTANCE_HIGH`); on iOS the only difference is the on-disk filename prefix (`custom_notification_v$gen.wav` vs. `custom_alarm_v$gen.wav`) under `Library/Sounds/`.
- File picker: `@Composable expect fun rememberSoundFilePicker` — Android uses `rememberLauncherForActivityResult(OpenDocument())` filtered to `audio/wav`; iOS uses `LocalUIViewController` to present `UIDocumentPickerViewController` with `UTType.wav`.
- Selected files are copied into app-private storage: Android writes to `<filesDir>/sounds/custom_sound_v$gen.wav` exposed via a `FileProvider` (authority `intellij.kmm.settings.grind_track.fileprovider`); iOS writes to `<App_Home>/Library/Sounds/custom_sound_v$gen.wav` so `UNNotificationSound.soundNamed(...)` resolves it. The version-suffix avoids both Android channel-id reuse (sounds are immutable post-creation) and iOS notification-sound caching.
- Android channel strategy: keep the default `rest_timer_alarm` channel for the default tone; on each new pick create `rest_timer_alarm_custom_$gen` with `USAGE_ALARM` audio attributes and the FileProvider URI, deleting the previous custom channel. `RestTimerAlarm.schedule` looks up the active channel id from `CustomSoundManager` and forwards it to `RestTimerReceiver` as an Intent extra. iOS reads the current `internalFilename` and applies `UNNotificationSound.soundNamed(...)` per-schedule.
- Persistence: `expect class SettingsStore` over `SharedPreferences` (Android) / `NSUserDefaults` (iOS). Three keys: `custom_sound_display_name`, `custom_sound_filename`, `custom_sound_generation`. No new dependencies.
- Format restriction: `.wav` only. iOS hard-caps notification sounds at 30 seconds; longer files silently fall back to the default tone — surfaced as static helper text in the Settings screen.

### Persistence
- Room v4, `BundledSQLiteDriver`, schemas exported to `composeApp/schemas`. Five entities: `Exercise`, `Routine`, `RoutineExercise` (with `targetReps: Int?` — null means "to failure"), `WorkoutSession`, `SetEntry`.
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
- **iOS alarm loudness.** The two-stage rest-end alert uses Time Sensitive Notifications for stage 2: breaks through Focus modes (DND/Sleep/Work focus) but not the physical mute switch. To get continuous-alarm-style behaviour that bypasses silent mode (matching Android), the Critical Alerts entitlement (`com.apple.developer.usernotifications.critical-alerts`) is required — gated behind Apple's manual approval at <https://developer.apple.com/contact/request/notifications-critical-alerts-entitlement/>, typically only granted for medical/public-safety apps. Not in scope; can be layered onto the existing two-stage code if approval is obtained.
- **No real Room migrations.** Acceptable while the app is alpha and there's no production data; before any external release this will need actual `Migration` objects.

## Convention reminders for future work

- New features go in `feature/<name>/{ui[, data, domain]}`. ViewModels via `koin-compose-viewmodel`, repositories via Koin singletons.
- Verify with `./gradlew :composeApp:check --no-daemon` (preapproved in `.claude/settings.local.json`). The JBR 21 `JAVA_HOME` workaround is in the same allowlist if Gradle ever fails on JDK version.
- Reach for `kotlin.time.Clock` / `kotlin.time.Instant` in new code rather than the deprecated `kotlinx.datetime` aliases.
