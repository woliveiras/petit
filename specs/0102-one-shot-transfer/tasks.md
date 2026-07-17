# Tasks: One-shot data transfer

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [ ] **Connect successful pairing to data transfer** (test-type: integration)
  - blocked-by: spec 0101
  - summary: preserve or explicitly re-establish the authorized endpoint when the user moves from pairing to send/receive.
  - desired behavior: completing pairing leads to an operable transfer instead of returning to a route that may have no connected endpoint.
  - acceptance criteria: after pairing, send/receive uses the authorized peer and does not fail with “No connected device found.”
  - verification: `./gradlew test`

- [x] **Serialize and submit a bundle to an existing endpoint** (test-type: integration)
  - blocked-by: an already connected Nearby endpoint
  - summary: build an `ExportBundle`, submit it as a Nearby payload, and expose merge/replace controls after a complete receive.
  - desired behavior: the existing components can initiate payload submission and present the available import choices.
  - acceptance criteria: serialization, payload submission, parsing, validation, and merge/replace entry points exist in the current app.
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

- [ ] **Display reliable progress, results, cancellation, and failures** (test-type: both)
  - blocked-by: bundle sending and import
  - summary: connect transferred bytes and the persisted result to interface states.
  - desired behavior: progress is monotonic, counters are exact, and cancellation or failure discards partial payloads.
  - acceptance criteria: cancellation or interruption persists no data and disconnects cleanly; completion shows actual quantities.
  - verification: `./gradlew test`

- [ ] **Validate end-to-end transfer on two devices** (test-type: integration)
  - blocked-by: transactional replace; progress, results, and failures
  - summary: run merge, replace, and interruption with and without an internet connection.
  - desired behavior: the final databases and interface match the selected option.
  - acceptance criteria: all spec scenarios pass on physical hardware.
  - verification: `./gradlew assembleDebug && ./gradlew installDebug`
