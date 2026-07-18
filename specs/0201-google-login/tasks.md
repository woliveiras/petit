# Tasks: Google Account Authentication

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until the Petit Cloud
> identity requirement is revalidated and explicitly approved.

## Tasks

- [ ] **Model independent Petit identity state** (test-type: unit)
  - blocked-by: approved Petit Cloud identity decision
  - desired behavior: authentication, Drive authorization, and entitlement cannot activate or clear one another.
  - acceptance criteria: state transitions preserve local access and an independent Drive authorization.
  - verification: `./gradlew test`

- [ ] **Authenticate with the selected provider** (test-type: both)
  - blocked-by: previous task; selected identity backend
  - desired behavior: a user can authenticate, cancel, fail, retry, and sign out without affecting local data.
  - acceptance criteria: every scenario in the spec is covered at the provider boundary and UI.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Verify coexistence with Google Drive authorization** (test-type: integration)
  - blocked-by: spec 0301; previous task
  - desired behavior: Petit Cloud sign-in and sign-out never grant or revoke Drive access.
  - acceptance criteria: all combinations of authenticated/unauthenticated and authorized/unauthorized remain distinguishable.
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
