# grind-track — Product Plan

## Product description

grind-track is a Kotlin Multiplatform (Android + iOS) gym workout tracker. The app supports three core flows:

1. **Build a routine.** The user creates a workout routine consisting of a sequence of exercises. Each entry in the routine specifies a target number of sets and reps and the rest interval to use after completing a set of that exercise.
2. **Run a workout.** The user picks a routine and the app replays it set by set. When the user marks a set complete, a rest timer starts automatically using the rest interval configured for that exercise. While the timer runs, the user enters the actual data they performed for that set (weight lifted, reps achieved). The data is persisted immediately.
3. **Track progress.** A dedicated screen reads the persisted set history and visualises progress — initially as a per-exercise list/chart of weight × reps over time.

The UI is organised as a three-tab bottom-navigation app: **Routines**, **Workout**, **Progress**.

## What is already implemented

This is the wizard-scaffolded baseline. None of the user-visible features are wired up yet, but the foundation is in place.

### Build & dependency setup
- Kotlin Multiplatform module `composeApp` targeting `androidTarget`, `iosArm64`, `iosSimulatorArm64`.
- Compose Multiplatform UI, Material3, lifecycle-viewmodel/runtime-compose, navigation-compose multiplatform.
- Room KMP with KSP wired for all three targets, schemas exported to `composeApp/schemas`.
- Koin DI (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android`).
- kotlinx-datetime + kotlinx-coroutines.
- Test deps in `commonTest`: kotlin-test, kotlinx-coroutines-test, turbine, koin-test.

### Persistence layer (Room)
All entities live in `core/database/entity/`:

| Entity | Fields | Notes |
|---|---|---|
| `Exercise` | `id`, `name`, `notes`, `defaultRestSeconds` | Catalogue of reusable exercises |
| `Routine` | `id`, `name`, `createdAt: Instant` | Workout template |
| `RoutineExercise` | `id`, `routineId`, `exerciseId`, `position`, `targetSets`, `restSecondsOverride?` | Join table; cascades on routine delete, restricts on exercise delete |
| `WorkoutSession` | `id`, `routineId?`, `startedAt`, `finishedAt?` | One run of a routine; `routineId` set NULL on routine delete |
| `SetEntry` | `id`, `sessionId`, `routineExerciseId`, `setIndex`, `weight: Double`, `reps: Int`, `completedAt` | Actual performed set; cascades on session/routineExercise delete |

`InstantTypeConverter` bridges `kotlinx.datetime.Instant` ↔ epoch millis. The database is `GymTrackDatabase` (version 1, `gymtrack.db`); construction goes through an `expect` `DatabaseFactory` with platform actuals in `androidMain`/`iosMain`. Driver is `BundledSQLiteDriver`.

DAOs implemented so far are intentionally minimal — `observeAll`/`observeForX` (`Flow`-returning) and `insert` only. No `update`, `delete`, or aggregate queries yet.

### DI
`AppModule.kt` (commonMain) exposes the database and all five DAOs as Koin singletons. `expect fun platformModule(): Module` is implemented per platform (Android wires `DatabaseFactory` with `androidContext`; iOS uses an NSFileManager-backed factory). `initKoin` is invoked from `GymTrackApplication.onCreate` on Android.

### UI shell
- `App()` (commonMain) → `GymTrackTheme` → `MainScreen()`.
- `MainScreen` hosts a Material3 `Scaffold` with a bottom `NavigationBar` and a `NavHost`.
- Three top-level destinations: `Routines`, `Workout`, `Progress` (`TopLevelDestination` sealed class).
- All three feature screens currently render only an `EmptyState` placeholder. No ViewModels exist yet.

### Identified schema gaps (will need migration before milestone 1 ships)
- `RoutineExercise` has `targetSets` but **no `targetReps`** (and no target weight). The product spec says routines define target reps, so we need to add at least `targetReps: Int` (and optionally `targetWeight: Double?`) and bump the database version with a migration.
- DAOs lack the mutation/aggregation queries the features will need (`update`, `delete`, latest-set-for-exercise, history-by-exercise, etc.).

---

## Implementation roadmap

The plan is organised into four milestones that each end in a usable slice of the product. Cross-cutting concerns (architecture conventions, testing) are listed once at the end.

### Milestone 1 — Routine management
**Outcome:** the user can create, edit, reorder, and delete routines and their exercises.

1. Schema update: add `targetReps: Int` (and `targetWeight: Double?` if desired) to `RoutineExercise`; bump `GymTrackDatabase` to version 2 and write a Room migration (or destructive migration during alpha).
2. Extend DAOs with `update`, `delete`, and ordered observation queries needed by the UI.
3. Add a `RoutineRepository` and `ExerciseRepository` in commonMain wrapping the DAOs (returning `Flow` for observation, `suspend` for mutations).
4. `RoutinesScreen` — list routines with a "+" FAB; tapping a routine opens a detail/edit screen where exercises can be added (chosen from `Exercise` catalogue or created inline), reordered, and have target sets/reps/rest set. Build the `RoutinesViewModel` and `RoutineEditorViewModel` using `koin-compose-viewmodel`.
5. Surface exercise CRUD either inline in routine editor or behind a small "Exercise library" screen.

### Milestone 2 — Replayable workout session
**Outcome:** the user can pick a routine, step through it set by set, and complete the workout.

1. `WorkoutRepository` to start/finish `WorkoutSession`s.
2. `WorkoutScreen` becomes a routine picker when no session is active. On start, transition to an in-session screen that displays the current exercise, current set index, and the target reps × sets.
3. `WorkoutViewModel` holds the session state machine: current exercise, current set, remaining sets, advance logic. The session state survives configuration changes (ViewModel) and process death (resume from DB by latest unfinished `WorkoutSession`).
4. "Mark set complete" advances the state machine and triggers the rest timer (built in milestone 3 — for now just no-op or a stub).

### Milestone 3 — Set logging + rest timer
**Outcome:** during a workout, finishing a set starts the per-exercise rest timer and prompts the user to log weight/reps; the entry is persisted as a `SetEntry`.

1. Rest timer: a small commonMain coroutine-based countdown driven by `restSecondsOverride ?: defaultRestSeconds`. UI shows remaining seconds; sound/haptic on completion can come later.
2. While the timer runs, the in-session screen reveals an inline form for weight + reps (defaulting to the previous set's values, or to last-known values for that exercise).
3. On submit (or on timer completion), insert a `SetEntry` via `SetEntryDao.insert`, advance the state machine, and reset the timer for the next set.
4. Edge cases: skipping a set, ending the session early (sets `finishedAt`), aborting without logging.

### Milestone 4 — Progress screen
**Outcome:** per-exercise progress over time is visible.

1. Add aggregation queries: latest `SetEntry` per exercise, history of `(weight, reps, completedAt)` for a given `exerciseId`, best (top set / e1RM) per session.
2. `ProgressViewModel` exposes a list of exercises (sorted by recent activity) and, on selection, the time series for that exercise.
3. `ProgressScreen` — an exercise picker plus a chart (initially a simple Compose canvas line chart of estimated 1RM or top-set weight; can be swapped for a charting lib later) and a tabular history.

### Cross-cutting

- **Architecture convention.** First feature (Milestone 1) sets the precedent: `feature/<name>/{data,domain,ui}` with ViewModels via `koin-compose-viewmodel`, repositories injected via Koin, screens stateless wherever possible (state hoisted to the ViewModel).
- **Testing.**
  - DAOs: use Room's in-memory builder + `BundledSQLiteDriver` in androidUnitTest / iosTest.
  - ViewModels: turbine on the exposed `StateFlow`; koin-test to provide fake repositories.
  - Aim for one test per ViewModel happy path and the most important error path before declaring a milestone done.
- **Verification.** Each milestone must keep `./gradlew :composeApp:check --no-daemon` green and a clean Android `assembleDebug` build.
