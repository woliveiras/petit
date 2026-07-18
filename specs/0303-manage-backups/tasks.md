# Tasks: Manage Google Drive Backups

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **Draft**. All implementation tasks remain pending until explicit approval.

## Tasks

- [ ] **List and inspect every recognized backup** (test-type: both)
  - blocked-by: spec 0301; spec approval
  - desired behavior: paginated Drive results become a sorted list with compatibility, content counts, trigger, and total size.
  - acceptance criteria: unknown files are ignored and empty, error, retry, and authorization states are distinguishable.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Delete one backup permanently** (test-type: both)
  - blocked-by: previous task
  - desired behavior: an exact selected Drive file is permanently deleted only after confirmation.
  - acceptance criteria: retry is idempotent, local data is unchanged, and the list refreshes.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Delete selected or all backups explicitly** (test-type: both)
  - blocked-by: previous task
  - desired behavior: users control bulk deletion without automatic retention or Petit limits.
  - acceptance criteria: exact IDs, bounded requests, partial failure reporting, and destructive confirmation are covered.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Disconnect and reconnect without deletion** (test-type: both)
  - blocked-by: first task
  - desired behavior: revoking Drive access hides remote data locally but leaves every backup intact.
  - acceptance criteria: reconnecting the same account restores the list and never requires Petit authentication.
  - verification: `./gradlew test` and physical-device provider validation
