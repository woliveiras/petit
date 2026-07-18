# Petit — Agent Instructions

This document is a technical guide for AI coding agents working in this repository.

## Repository Scope

- Android app in Kotlin.
- Jetpack Compose for UI.
- MVVM + Repository architecture.
- Room as local source of truth.
- Hilt for dependency injection.

## Module

| Module | Type                    | Package              |
| ------ | ----------------------- | -------------------- |
| app    | com.android.application | com.woliveiras.petit |

## Tech Stack

> Canonical versions are in `gradle/libs.versions.toml`.

- Kotlin (JVM 17)
- Jetpack Compose (Material 3)
- Room
- Hilt
- WorkManager
- Navigation Compose
- DataStore
- Coroutines
- Vico (charts)
- Coil (image loading)
- Google Nearby Connections (family group sync)

## Build Commands

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew assembleDebug && ./gradlew installDebug
./gradlew test
./gradlew spotlessCheck
./gradlew spotlessApply
```

## Agent Workflow Rule

When running assembleDebug, run installDebug right after.

## Code Conventions

- Kotlin only.
- Compose for UI.
- ViewModels depend on repositories, not DAOs.
- Room entities include createdAt, updatedAt, deletedAt, syncStatus.
- Room active queries filter deletedAt IS NULL.

## Security Baseline

- Never commit secrets or credentials.
- Keep local.properties, keystore.properties, and google-services.json out of version control.
- Validate external inputs (forms/import files) before persisting.
- Use immutable PendingIntent flags when applicable.

## Technical Structure

app/src/main/java/com/woliveiras/petit/

- data/local/db, data/local/dao, data/local/entity
- data/mapper
- data/repository
- domain/model, domain/usecase
- presentation/feature, presentation/components, presentation/navigation, presentation/util
- ui/theme
- worker
- di

## Documentation Policy

- All repository documentation, including embedded code comments and examples,
  must be written in English.
- Public technical documentation stays in docs/.
- Product strategy and planning are maintained in a separate repository.
