# Petit Testing Guide

This guide explains how to run project tests quickly.

## Prerequisites

1. Java 21 installed (`java -version`)
2. Android SDK configured (`ANDROID_HOME` / `ANDROID_SDK_ROOT`)
3. Emulator/device for instrumented tests

See `README.md` for environment setup.

## Test Types

| Type         | Location               | Description                          |
| ------------ | ---------------------- | ------------------------------------ |
| Unit         | `app/src/test/`        | Fast JVM tests, no emulator required |
| Instrumented | `app/src/androidTest/` | Runs on Android device/emulator      |

## Run Unit Tests

```bash

./gradlew test
```

Verbose run:

```bash
./gradlew test --info
```

Open unit test report:

```bash
open app/build/reports/tests/testDebugUnitTest/index.html
```

## Run Instrumented Tests

Start emulator:

```bash
emulator -list-avds
emulator -avd Pixel_7_API_34 &
```

Verify device:

```bash
adb devices
```

Run instrumented tests:

```bash

./gradlew connectedAndroidTest
```

Open report:

```bash
open app/build/reports/androidTests/connected/index.html
```

## Run Specific Test Class

```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.woliveiras.petit.data.local.dao.CatDaoTest
```

## Command Summary

| Action             | Command                          |
| ------------------ | -------------------------------- |
| Unit tests         | `./gradlew test`                 |
| Instrumented tests | `./gradlew connectedAndroidTest` |
| List AVDs          | `emulator -list-avds`            |
| Start emulator     | `emulator -avd <AVD_NAME> &`     |
| Devices            | `adb devices`                    |

## Troubleshooting

### No connected devices

- Start emulator and wait for boot completion.
- Re-run `adb devices`.

### Build/runtime test errors

Run a clean sequence:

```bash
./gradlew clean
./gradlew assembleDebugAndroidTest
./gradlew connectedAndroidTest
```

### Missing reports

If report files are not generated, tests likely failed before report generation. Re-run and inspect terminal output.
