# Tasks: Device pairing

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Provide discovery and group persistence** (test-type: integration)
  - blocked-by: none
  - summary: integrate Nearby, permissions, UI, repositories, and DataStore.
  - desired behavior: the app starts or stops advertising/discovery and retains the local group identity.
  - acceptance criteria: the components exist in the current flow without a cloud dependency.
  - verification: `./gradlew test`

- [ ] **Authorize pairing with a four-digit code** (test-type: both)
  - blocked-by: discovery and group persistence
  - summary: generate, display, receive, validate, and expire the code.
  - desired behavior: only the receiver with the correct code completes pairing.
  - acceptance criteria: the correct code connects; an incorrect code is rejected; another attempt is allowed.
  - verification: `./gradlew test`

- [ ] **Make cancellation and failures atomic** (test-type: both)
  - blocked-by: code authorization
  - summary: clean up advertising, discovery, the connection, and incomplete state.
  - desired behavior: interruptions never leave a partially persisted key or member.
  - acceptance criteria: cancelling or losing the endpoint returns to the initial state without residual data.
  - verification: `./gradlew test`

- [ ] **Validate end-to-end pairing on two devices** (test-type: integration)
  - blocked-by: code authorization; atomic cancellation and failures
  - summary: run the acceptance matrix on physical hardware.
  - desired behavior: pairing works with and without an internet connection and respects permissions.
  - acceptance criteria: two devices persist the same key; an invalid code and cancellation do not pair them.
  - verification: `./gradlew assembleDebug && ./gradlew installDebug`
