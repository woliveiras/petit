# Development setup — Android Studio

This guide covers the recommended setup for working on Petit in **Android Studio**.

For the VS Code-based setup, see [VS Code](vscode.md).

## 1. Prerequisites

- Android Studio (Hedgehog or newer recommended; any version that supports AGP 8.x)
- Bundled JDK 17+ (Android Studio ships its own JDK; the project targets JVM 17)
- Android SDK with at least:
  - Platform `android-34`
  - Build-tools `35.0.1`
  - Platform-tools
  - Emulator + a system image (or a physical device with USB debugging)

Android Studio installs SDK components through **Settings → Languages & Frameworks → Android SDK**.

## 2. Open the project

1. **File → Open** and select the repository root.
2. Wait for the initial Gradle sync. AGP and Kotlin versions are pinned in [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml); do not upgrade through the IDE prompts without coordination.
3. If Studio prompts to use a different JDK, point it at a JDK 17 (or the bundled one). The project's `org.gradle.java.home` is **not** committed.

## 3. Run configurations

Studio auto-creates an `app` run configuration. The defaults are correct:

- Module: `app`
- Build variant: `debug`
- Application id: `com.woliveiras.petit.debug`
- Launch activity: `com.woliveiras.petit.MainActivity`

Use **Run ▶** (or **Debug**) to install and launch on the selected device/emulator.

## 4. Emulator

Create AVDs through **Tools → Device Manager**. A Pixel 7 / API 34 image is the team default and matches the VS Code task `Start Emulator`.

## 5. Tests

- **Unit tests**: right-click `app/src/test` → **Run 'Tests in test'**, or `./gradlew test`.
- **Instrumented tests**: right-click `app/src/androidTest` → **Run** (requires a running device/emulator), or `./gradlew connectedAndroidTest`.

## 6. Formatting

The project uses **Spotless** as the source of truth for Kotlin formatting:

```bash
./gradlew spotlessCheck
./gradlew spotlessApply
```

Studio's built-in Kotlin formatter and Spotless can disagree. Run `spotlessApply` before pushing to avoid CI failures.

## 7. Useful Gradle commands

The same commands work from Studio's terminal or any shell:

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew spotlessCheck
```

> Per repo convention: after `assembleDebug`, run `installDebug`.

## 8. Troubleshooting

- **Gradle sync fails on JDK** — set Studio's Gradle JDK to a JDK 17+ install (Settings → Build Tools → Gradle).
- **AGP upgrade prompt** — decline unless the change is coordinated; versions live in [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml).
- **Hilt / KSP errors after pulling** — run **Build → Clean Project** then **Rebuild**, or `./gradlew clean assembleDebug`.
- **Compose preview not rendering** — ensure the IDE Compose plugin is enabled and run a full build first.
