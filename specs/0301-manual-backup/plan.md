# Plan: Manual Google Drive Backup

Spec: [spec.md](./spec.md)

## Status

This plan is **Approved** and may be implemented.

## Dependencies

- Spec 0006 for the existing export/import domain model.
- Spec 0204 for the approved free user-owned-cloud boundary.
- [Google Cloud Console setup runbook](../../docs/test-runbooks/google-drive-cloud-console-setup.md).

Spec 0201 and Petit Cloud authentication are explicitly not dependencies.

## Architecture

- `GoogleDriveAuthorizationRepository` owns disconnected, authorizing,
  authorized, needs-authorization, and unavailable states.
- Google Identity Services `AuthorizationClient` requests `drive.appdata`
  just in time and provides short-lived access tokens.
- `BackupArchiveCodec` creates and validates the ZIP and `manifest.json`.
- `CreateBackupSnapshotUseCase` reads all restorable Room data in one transaction
  and stages app-owned assets.
- `DriveBackupStorageRepository` uploads, resumes, and verifies archives in
  `appDataFolder` through a provider-neutral gateway.
- `CreateManualBackupUseCase` orchestrates snapshot, archive, upload, progress,
  cleanup, and history.

## Implementation sequence

1. Characterize the current JSON export/import and enumerate every restorable field and asset.
2. Define the versioned manifest, safe ZIP paths, checksums, and resource limits.
3. Build a transactionally consistent snapshot and portable asset staging area.
4. Add Google Drive authorization without Firebase or Petit authentication.
5. Implement appDataFolder resumable upload with deterministic backup IDs.
6. Deliver Settings UI, cancellation, progress, retry, and accessible error states.
7. Validate the real Google Cloud project and physical-device flow.

## Error and cleanup rules

- Cancellation always removes local staging files.
- A failed upload never appears as a completed backup in Petit.
- Retry reuses the same backup ID until completion is known.
- Authorization-required results stop background work and require foreground resolution.
- Quota errors are not retried automatically without a state change.
- Secrets, tokens, and clinical values never appear in logs.

## Planned verification

- Focused tests after each vertical behavior.
- Drive gateway contract tests with a deterministic fake server.
- Room and filesystem integration tests for snapshot consistency and cleanup.
- Instrumented authorization and UI tests.
- Manual execution of the linked Cloud Console runbook.
- `./gradlew spotlessCheck`
- `./gradlew test`
- `./gradlew connectedDebugAndroidTest` when a compatible device is available.

## Official references

- [Authorize access to Google user data on Android](https://developer.android.com/identity/authorization)
- [Store application-specific data in Google Drive](https://developers.google.com/workspace/drive/api/guides/appdata)
- [Upload file data](https://developers.google.com/workspace/drive/api/guides/manage-uploads)
- [Resolve Google Drive API errors](https://developers.google.com/workspace/drive/api/guides/handle-errors)
