# Development setup — VS Code

This guide covers the recommended setup for working on Petit in **VS Code**, without Android Studio.

For the alternative IDE setup, see [Android Studio](android-studio.md).
For deeper CLI tooling details (SDK install, emulator, etc.), see [Android CLI development](../android-cli-development.md).

## 1. Prerequisites

- macOS, Linux, or Windows (WSL2 recommended)
- Java 17+ (project targets JVM 17 — Java 21 also works)
- Android SDK with platform-tools and an emulator or physical device
- ADB on `$PATH`

Recommended Java install via SDKMAN:

```bash
curl -s "https://get.sdkman.io" | bash
sdk install java 21.0.3-tem
sdk default java 21.0.3-tem
```

Add the Android SDK to your shell:

**MacOS**:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

**Linux**:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

Full SDK component installation is documented in [android-cli-development.md](../android-cli-development.md).

## 2. VS Code extensions

The repo ships a workspace recommendation list in [.vscode/extensions.json](../../.vscode/extensions.json). When you open the project, VS Code will offer to install them.

Currently recommended:

- **Kotlin** (`jetbrains.kotlin`) — the official JetBrains extension. Bundles a Kotlin Language Server with proper Compose support. See section 3 for installation.
- **Gradle for Java** (`vscjava.vscode-gradle`) — task runner and Gradle integration.
- **Gradle Language Support** (`naco-siren.gradle-language`).
- **Extension Pack for Java** (`vscjava.vscode-java-pack`) and **Language Support for Java** (`redhat.java`) — needed by the Gradle / Kotlin tooling and for any Java sources.
- **Android** (`adelphes.android-dev-ext`) — logcat, device explorer, and APK install helpers.
- **XML** (`dotjoshjohnson.xml`) — for Android resource files.

> Avoid installing `fwcd.kotlin` or `mathiasfrohlich.Kotlin` alongside the JetBrains extension — they conflict on Kotlin file ownership.

## 3. Install the JetBrains Kotlin extension

The JetBrains Kotlin extension is **not on the VS Code Marketplace**. Install it manually from the official site:

1. Download the `.vsix` from JetBrains:
   <https://github.com/Kotlin/kotlin-lsp/releases>
   (or the link published at <https://blog.jetbrains.com/>; pick the latest release for VS Code).
2. In VS Code: **Extensions view → `…` menu → Install from VSIX…** and select the downloaded file.

   Or via CLI:

   ```bash
   code --install-extension /path/to/kotlin-<version>.vsix
   ```

3. Reload the VS Code window.

The bundled language server is used automatically — no extra path configuration is needed. The workspace [`.vscode/settings.json`](../../.vscode/settings.json) already enables it:

```jsonc
{
  "kotlin.languageServer.enabled": true,
  "kotlin.compiler.jvm.target": "17",
}
```

> First indexing on a fresh checkout takes a few minutes. Wait for indexing to finish before relying on completions.

## 4. Workspace tasks

Common Gradle commands are wired up in [`.vscode/tasks.json`](../../.vscode/tasks.json). Run them with **`Cmd/Ctrl + Shift + P` → Tasks: Run Task**:

| Task           | Command                                                                                |
| -------------- | -------------------------------------------------------------------------------------- |
| Build Debug    | `./gradlew :app:assembleDebug`                                                         |
| Install Debug  | `./gradlew :app:installDebug`                                                          |
| Run App        | `./gradlew :app:installDebug` + `adb shell am start -n com.woliveiras.petit.debug/...` |
| Run Tests      | `./gradlew :app:test`                                                                  |
| Clean          | `./gradlew clean`                                                                      |
| Start Emulator | `emulator -avd Pixel_7_API_34`                                                         |

`Build Debug` is the default build task (`Cmd/Ctrl + Shift + B`).

> Per repo convention, after `assembleDebug` you should run `installDebug`. The `Run App` task does both and launches the activity in one step.

## 5. Run on a device/emulator

1. Start an emulator (Task: **Start Emulator**) or plug in a device with USB debugging enabled.
2. Confirm it is visible:

   ```bash
   adb devices
   ```

3. Run the **Run App** task.

The debug app id is `com.woliveiras.petit.debug` and the launcher activity is `com.woliveiras.petit.MainActivity`.

## 6. Tests and formatting

```bash
./gradlew test            # unit tests
./gradlew connectedAndroidTest   # instrumented tests (needs device)
./gradlew spotlessCheck   # style check
./gradlew spotlessApply   # auto-fix style
```

Spotless is the source of truth for formatting. VS Code's Kotlin formatter is disabled in workspace settings to avoid drift.

## 7. Troubleshooting

- **No Kotlin completions** — confirm the JetBrains Kotlin extension is installed and active, then reload the window. First-time indexing can take a few minutes.
- **Gradle sync errors** — confirm `JAVA_HOME` resolves to JDK 17+ (`java -version`).
- **`adb` not found** — make sure `platform-tools` is on your `PATH`.
- **Emulator boots but app doesn't install** — run `./gradlew installDebug` directly to see the full Gradle error.
