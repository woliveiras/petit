# Tasks: Backup Settings

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **In Progress**. Provider-independent implementation is underway.

## Tasks

- [x] **Persist free backup preferences** (test-type: both)
  - blocked-by: spec 0305
  - desired behavior: automatic, network, and notification preferences survive restart without credentials, gates, retention, or exact-time fields.
  - acceptance criteria: defaults and every update are covered in DataStore integration tests.
  - verification: `./gradlew test`

- [x] **Control one unique automatic schedule** (test-type: both)
  - blocked-by: previous task
  - desired behavior: enable, disable, and connected/unmetered changes update WorkManager without duplication.
  - acceptance criteria: UI describes execution as inexact and manual backup remains independent.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [x] **Expose manual backup, history, and notifications** (test-type: both)
  - blocked-by: previous task; spec 0303
  - desired behavior: Settings starts a manual backup, displays non-clinical history, and applies the success-notification preference.
  - acceptance criteria: success, retry, failure, and authorization-required states are localized and accessible.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [~] **Disconnect Drive without deleting data** (test-type: both)
  - blocked-by: previous tasks
  - desired behavior: confirmation revokes Drive access and cancels work while preserving local and remote backups.
  - acceptance criteria: reconnect works without a Petit account and restores the remote list.
  - verification: `./gradlew test` and physical-device provider validation
