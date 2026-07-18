# Plan: Restore Google Drive Backup

Spec: [spec.md](./spec.md)

## Status

This plan is **Approved** and may be implemented.

## Dependencies

- Spec 0301 archive, authorization, and Drive gateway contracts.
- Existing transactional MERGE/REPLACE import and shared conflict resolver.

## Architecture

- `DownloadBackupUseCase` streams the selected archive into private staging.
- `BackupArchiveCodec` performs structural, size, path, manifest, checksum, and
  schema validation without extracting unsafe paths.
- `RestoreBackupUseCase` prepares entity and asset changes before entering the
  final transaction boundary.
- `RestoreInstaller` coordinates unique asset staging, transactional reference
  installation, recovery cleanup, and reminder rescheduling.

## Implementation sequence

1. Add hostile-archive characterization fixtures and limits.
2. Implement download, cancellation, progress, and staging cleanup.
3. Validate every archive entry and the complete export model.
4. Implement exact REPLACE with two-phase asset installation and recovery tests.
5. Implement MERGE with the shared resolver and winning-asset rules.
6. Restore preferences and reschedule reminders according to the selected mode.
7. Deliver accessible confirmation, progress, success, and recovery states.
8. Validate restoration on a second physical device.

## Planned verification

- ZIP slip, ZIP bomb, duplicate path, missing entry, checksum, and schema tests.
- Room transaction, asset staging, orphan cleanup, and process-death recovery tests.
- MERGE/REPLACE regression suites.
- Instrumented new-device restore journey.
- `./gradlew spotlessCheck`
- `./gradlew test`
- `./gradlew connectedDebugAndroidTest` when available.
