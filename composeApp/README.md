# composeApp — GymTrack shared module

Kotlin Multiplatform module hosting all shared code (data, domain, UI) for the
GymTrack offline gym-tracking app, plus the Android entry point and the
exposed iOS framework.

## Conventions

### Package name

The spec called for `com.example.gymtrack`, but the project skeleton was
already generated under **`intellij.kmm.settings.grind_track`** — that name
appears in the Android `applicationId` and `namespace`, the iOS
`PRODUCT_BUNDLE_IDENTIFIER`, the generated Compose-resources package, and the
Swift bridge (`MainViewControllerKt`). Renaming would touch every source file
plus Gradle, the manifest, and `Config.xcconfig` for no functional gain, so
the existing root package is kept and the spec'd subpackages live under it:

```
intellij.kmm.settings.grind_track/
├── app/                  # Entry point, navigation graph, top-level screen
├── core/
│   ├── database/         # Room entities, DAOs, GymTrackDatabase, factory
│   ├── designsystem/     # Theme, reusable building blocks
│   ├── domain/           # (added in later milestones)
│   └── util/             # (added in later milestones)
├── feature/
│   ├── routines/
│   ├── workout/
│   └── progress/
└── di/                   # Koin modules + initKoin()
```

### Persistence

Room is configured for KMP using the `@ConstructedBy` /
`RoomDatabaseConstructor` pattern. The bundled SQLite driver
(`androidx.sqlite:sqlite-bundled`) is used on every platform; the actual file
location is provided by an `expect class DatabaseFactory` whose Android
`actual` resolves the app database directory and whose iOS `actual` writes to
the app sandbox `Documents/` directory.

Weights are stored in **kilograms**. A user-facing kg/lb display toggle is
deferred to Milestone 5.

### Dependency injection

Koin (`koin-core`, `koin-compose`, `koin-compose-viewmodel`,
`koin-android`). `initKoin()` is invoked once from
`GymTrackApplication.onCreate()` on Android and from `MainViewController`'s
`configure` block on iOS; a `GlobalContext.getOrNull()` guard keeps repeated
calls (preview/hot-reload) safe.

### Navigation

JetBrains Navigation Compose Multiplatform (`navigation-compose 2.9.2`),
single `NavHost` driven by `MainScreen`'s `Scaffold` + `NavigationBar`.

## Build

`./gradlew :composeApp:check` runs lint + unit tests for both Android and
common source sets.

### JDK requirement

The Gradle wrapper (8.14.3) ships an embedded Kotlin (2.0.21) that cannot
parse JDK 25's version string, while many devs default to the bundled
JetBrains Runtime 25. `gradle/gradle-daemon-jvm.properties` pins the daemon
to `toolchainVersion=21`, so Gradle auto-discovers a JDK 21 install (e.g.
JBR 21 from `~/Library/Java/JavaVirtualMachines/`) without needing
`JAVA_HOME` to be set. Make sure at least one JDK 21 is installed locally.
