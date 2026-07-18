# Backup provider boundary

Petit backup behavior is split into a provider-independent archive/application layer and a replaceable remote-storage adapter.

## Implemented boundary

- `BackupArchiveCodec` creates and validates the versioned portable ZIP contract.
- `BackupArchivePreparer` captures the Room snapshot, preferences, reminder settings, and app-owned pet assets.
- `BackupAuthorizationGateway` exposes authorization state and a foreground authorization operation without provider SDK types.
- `BackupStorageGateway` exposes exact-ID upload, download, paginated list, metadata lookup, and deletion operations.
- `CreateBackupAction` is shared by manual UI and background workers and requires a stable backup ID for retry idempotency.
- ViewModels and workers consume only these provider-independent contracts.

The deterministic provider is located only in `app/src/test`. It exercises success, pagination, authorization loss, quota, interruption, retryable and permanent failures, lost upload responses, and idempotent retries. It cannot be injected into a production component.

## Production behavior without a provider

This build binds authorization and backup creation to explicit unavailable implementations. Settings explains that Google Drive backup is not configured, and the backup action is disabled. No local fake can report a production cloud upload as successful.

## Real Google adapter entry point

A later implementation should:

1. implement `BackupAuthorizationGateway` with foreground Google authorization for only `drive.appdata`;
2. implement `BackupStorageGateway` with `appDataFolder` resumable upload and exact-ID operations;
3. bind those adapters and construct `CreateManualBackupUseCase` from the existing `BackupArchivePreparer`;
4. retain WorkRequest and ViewModel backup IDs across retries;
5. run the real-account and physical-device runbooks before checking any Google-specific task.

The adapter must not introduce Google SDK types into domain use cases, ViewModels, workers, or Compose UI.
