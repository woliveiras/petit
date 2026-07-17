# Tasks: One-shot data transfer

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Send and import a paired bundle** (test-type: integration)
  - blocked-by: spec 0101
  - summary: serialize, transport, and present merge/replace options.
  - desired behavior: the receiver gets the shareable dataset and chooses how to apply it.
  - acceptance criteria: the flow and corresponding components exist in the current app.
  - verification: `./gradlew test`

- [x] **Merge entities by identity and update time** (test-type: unit)
  - blocked-by: received bundle
  - summary: insert new UUIDs and keep the version with the latest `updatedAt`.
  - desired behavior: reapplying the same bundle does not duplicate records.
  - acceptance criteria: the current merge strategy applies UUID + `updatedAt`.
  - verification: `./gradlew test`

- [ ] **Replace the shareable database transactionally** (test-type: both)
  - blocked-by: bundle validation
  - summary: remove missing local records and import only the received content.
  - desired behavior: replace reflects the bundle exactly or changes nothing on failure.
  - acceptance criteria: missing local records cease to exist; failure rolls back the entire operation.
  - verification: `./gradlew test`

- [ ] **Display reliable progress, results, and failures** (test-type: both)
  - blocked-by: bundle sending and import
  - summary: connect transferred bytes and the persisted result to interface states.
  - desired behavior: progress is monotonic, counters are exact, and partial payloads are discarded.
  - acceptance criteria: interruption persists no data; completion shows actual quantities.
  - verification: `./gradlew test`

- [ ] **Validate end-to-end transfer on two devices** (test-type: integration)
  - blocked-by: transactional replace; progress, results, and failures
  - summary: run merge, replace, and interruption with and without an internet connection.
  - desired behavior: the final databases and interface match the selected option.
  - acceptance criteria: all spec scenarios pass on physical hardware.
  - verification: `./gradlew assembleDebug && ./gradlew installDebug`
