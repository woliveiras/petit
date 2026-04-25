# Native Android with Kotlin + Compose - CLI Development

Complete guide to develop Android apps without opening Android Studio, using only VS Code and command line tools.

## Index

1. Prerequisites and installation
2. Environment setup
3. Project bootstrap
4. Emulator management
5. Build and run
6. Testing
7. VS Code setup
8. Useful commands
9. Troubleshooting

## 1. Prerequisites and Installation

### macOS

```bash
# 1) Install Homebrew (if needed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2) Install SDKMAN (Java version manager)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 3) Install Java 21
sdk install java 21.0.3-tem
sdk default java 21.0.3-tem

# 4) Install Android command line tools
brew install --cask android-commandlinetools

# 5) Verify
java -version
sdkmanager --version
```

### Why SDKMAN

- Manage multiple Java versions safely.
- Switch versions per project with `.sdkmanrc`.
- Avoid conflicts between unrelated projects.

Useful SDKMAN commands:

```bash
sdk list java
sdk install java 17.0.11-tem
sdk use java 17.0.11-tem
echo "java=21.0.3-tem" > .sdkmanrc
sdk env
```

> AGP 8.x requires JDK 17+; Java 21 is fully compatible.

## 2. Environment Setup

### 2.1 Shell Variables

Add to your `~/.zshrc`:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/emulator:$PATH"
```

Reload shell:

```bash
source ~/.zshrc
```

### 2.2 Install Android SDK Components

```bash
yes | sdkmanager --licenses
sdkmanager "platforms;android-34"
sdkmanager "build-tools;35.0.1"
sdkmanager "platform-tools"
sdkmanager "emulator"

# Intel/AMD Macs
sdkmanager "system-images;android-34;google_apis;x86_64"

# Apple Silicon
sdkmanager "system-images;android-34;google_apis;arm64-v8a"

sdkmanager --list_installed
```

### 2.3 Verify Tooling

```bash
adb version
emulator -version
sdkmanager --version
```

## 3. Project Bootstrap

Create project structure:

```bash
mkdir -p app/src/main/java/com/woliveiras/petit
mkdir -p app/src/main/java/com/woliveiras/petit
mkdir -p app/src/main/res/values
mkdir -p app/src/test/java/com/woliveiras/petit
mkdir -p app/src/androidTest/java/com/woliveiras/petit
mkdir -p gradle/wrapper
```

Core files to create:

- `settings.gradle.kts`
- `build.gradle.kts` (root)
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `gradle.properties`

Recommended Android config baseline:

- `compileSdk = 36`
- `targetSdk = 34`
- `minSdk = 26`
- Java/Kotlin target = 17

## 4. Emulator Management

List AVDs:

```bash
emulator -list-avds
```

Start AVD:

```bash
emulator -avd Pixel_7_API_34 &
```

Verify connected device:

```bash
adb devices
```

## 5. Build and Run

From the project root:

```bash

./gradlew assembleDebug
./gradlew installDebug
adb shell am start -n com.woliveiras.petit.debug/com.woliveiras.petit.MainActivity
```

## 6. Testing

Unit tests:

```bash
./gradlew test
```

Instrumented tests (requires device/emulator):

```bash
./gradlew connectedAndroidTest
```

## 7. VS Code Setup

Recommended extensions:

- Kotlin language support
- EditorConfig
- Markdown support

Recommended workflow:

- Use Gradle commands for build/test/format.
- Avoid relying on editor auto-format for large modules.

## 8. Useful Commands

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew clean
./gradlew dependencies
./gradlew spotlessApply
./gradlew spotlessCheck
```

## 9. Troubleshooting

### Cannot find AVD system path

Check `ANDROID_SDK_ROOT`:

```bash
echo $ANDROID_SDK_ROOT
```

### Gradle wrapper class not found

Regenerate wrapper:

```bash
brew install gradle
gradle wrapper --gradle-version=8.11.1
```

### Build fails with compileSdk mismatch

Ensure `compileSdk = 36` in `app/build.gradle.kts`.

### Device not detected

- Re-run `adb devices`.
- Restart ADB:

```bash
adb kill-server
adb start-server
```

## References

- [Android Command Line Tools](https://developer.android.com/studio#command-tools)
- [Gradle User Manual](https://docs.gradle.org/current/userguide/userguide.html)
- [Kotlin Android Documentation](https://kotlinlang.org/docs/android-overview.html)
