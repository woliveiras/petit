# Tasks: Automatic Google Drive Backup

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **In Progress**. Provider-independent implementation is underway.

## Tasks

- [x] **Schedule one inexact daily backup** (test-type: both)
  - blocked-by: spec 0301
  - desired behavior: explicit opt-in enqueues one unique periodic request and opt-out cancels it.
  - acceptance criteria: duplicate enable calls, app restart, constraint changes, and cancellation preserve one authoritative schedule.
  - verification: `./gradlew test`
  - evidence: WorkManager tests recreate the scheduler, update the unique request, verify approved constraints and backoff, and cancel it.

- [~] **Run the shared backup flow in background** (test-type: both)
  - blocked-by: previous task
  - desired behavior: the worker creates the same complete archive as manual backup and uploads it after process death.
  - acceptance criteria: success, cancellation, transient retry, quota, and permanent failures produce accurate history without duplicate backups.
  - verification: `./gradlew test`
  - provider-independent evidence: worker integration uses the real shared use case and deterministic gateway; physical process-death validation remains open.

- [~] **Require foreground reauthorization safely** (test-type: both)
  - blocked-by: previous task
  - desired behavior: a worker that needs user interaction stops without opening UI and exposes a reconnect action.
  - acceptance criteria: Drive revocation never falls back to Petit login and never deletes local or remote data.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
  - provider-independent evidence: the worker records authorization required without preparing an archive or launching interactive UI; foreground reconnect remains open.

- [ ] **Validate Android background behavior** (test-type: integration)
  - blocked-by: previous tasks
  - desired behavior: work remains durable and respects constraints, backoff, Doze, and process death.
  - acceptance criteria: physical evidence confirms inexact execution and no exact-time promise or automatic retention.
  - verification: physical-device background runbook created during implementation
