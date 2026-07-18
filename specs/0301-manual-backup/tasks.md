# Tasks: Manual Google Drive Backup

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **In Progress**. Provider-independent implementation is underway.

## Tasks

- [x] **Create a complete portable backup archive** (test-type: both)
  - blocked-by: none
  - desired behavior: one Room snapshot produces a versioned ZIP containing all restorable data and app-owned pet assets.
  - acceptance criteria: manifest payload paths, sizes, counts, SHA-256 checksums, JSON data, assets, exclusions, final archive metadata, and cleanup match the archive contract.
  - verification: `./gradlew test`

- [ ] **Authorize Google Drive independently** (test-type: both)
  - blocked-by: previous task; completed Cloud Console runbook
  - desired behavior: a user without a Petit account can grant, cancel, retry, disconnect, and recover Drive app-data authorization.
  - acceptance criteria: only `drive.appdata` is requested; no purchase, Firebase session, or Petit account is created.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
  - provider-independent progress: authorization states and foreground consent contracts are implemented; a production Google authorization adapter remains open.

- [ ] **Upload one backup idempotently** (test-type: both)
  - blocked-by: previous tasks
  - desired behavior: a resumable upload publishes one completed archive per backup ID and reports monotonic byte progress.
  - acceptance criteria: interruption, timeout, retry, cancellation, token expiry, and quota errors cannot produce duplicate completed backups or leak staging files.
  - verification: `./gradlew test`
  - provider-independent progress: stable IDs, monotonic progress, typed failures, cleanup, and retry idempotency are verified against a deterministic test-only provider; real resumable upload remains open.

- [ ] **Expose manual backup states in Settings** (test-type: both)
  - blocked-by: previous task
  - desired behavior: Settings explains authorization, starts backup, shows progress and completion, and offers actionable recovery.
  - acceptance criteria: all scenarios in `spec.md` have localized, accessible UI coverage.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
  - provider-independent progress: localized unavailable, authorization, progress, completion, quota, retry, and failure states are implemented; the production build fails closed until a real provider is configured.

- [ ] **Validate the real provider integration** (test-type: integration)
  - blocked-by: previous tasks
  - desired behavior: debug and release identities can authorize and upload to their own appDataFolder configuration.
  - acceptance criteria: consent, cancellation, revocation, offline behavior, retry, and quota handling are evidenced on a physical device.
  - verification: [Google Cloud Console setup runbook](../../docs/test-runbooks/google-drive-cloud-console-setup.md) and the provider test runbook created during implementation
