# grind-track

A Kotlin Multiplatform (Android + iOS) gym workout tracker.

## What it does

grind-track supports four user flows:

1. **Build a routine.** Create a workout routine as an ordered sequence of exercises with target sets/reps and a per-exercise rest interval.
2. **Replay it.** Pick a routine and step through it set by set during a workout session.
3. **Log sets.** When a set is marked complete, a rest timer specific to that exercise starts; during the rest the user enters the actual weight and reps performed, which is persisted as a `SetEntry`.
4. **Track progress.** A progress screen reads the persisted history and visualises per-exercise improvement over time.

The UI is a three-tab bottom-navigation app: **Routines**, **Workout**, **Progress**.

For the full product spec, current implementation status, and milestone roadmap see [`docs/PLAN.md`](./docs/PLAN.md).

## Tech stack

- **Kotlin Multiplatform** (Android, iOS arm64, iOS simulator arm64), Kotlin 2.3, JVM target 11.
- **Compose Multiplatform** for shared UI; Material3, navigation-compose multiplatform.
- **Room KMP** with KSP, schemas exported to `composeApp/schemas`, `BundledSQLiteDriver` on all platforms.
- **Koin** for DI (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android`).
- **kotlinx-datetime** + **kotlinx-coroutines**.
- Tests: kotlin-test, kotlinx-coroutines-test, turbine, koin-test.

Authoritative versions live in [`gradle/libs.versions.toml`](./gradle/libs.versions.toml).

## Repository layout

- [`composeApp/src/commonMain`](./composeApp/src/commonMain/kotlin) — shared Kotlin code (UI, ViewModels, repositories, DB entities/DAOs, DI).
- [`composeApp/src/androidMain`](./composeApp/src/androidMain/kotlin) — Android entry point (`MainActivity`, `GymTrackApplication`) and `expect`/`actual` platform code.
- [`composeApp/src/iosMain`](./composeApp/src/iosMain/kotlin) — iOS entry point (`MainViewController`) and platform `actual`s.
- [`iosApp/`](./iosApp) — the iOS app shell (Swift / Xcode project) that hosts the shared Compose UI.

## Build and run

### Android

```shell
./gradlew :composeApp:assembleDebug
```

(Use `gradlew.bat` on Windows.) Install/run via the IDE's run widget or `adb install` the resulting APK.

### iOS

Open [`iosApp/`](./iosApp) in Xcode and run from there. There is no Gradle target that builds the iOS app end-to-end.

### Verify the build

```shell
./gradlew :composeApp:check --no-daemon
```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html).
