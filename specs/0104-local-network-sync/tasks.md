# Tasks: Local network sync

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [ ] **Define a versioned, authenticated local protocol** (test-type: unit)
  - blocked-by: approved specs 0101 and 0103
  - summary: model messages, limits, errors, ACK, and the rule for simultaneous sessions.
  - desired behavior: no entity is sent before validating the version, key, and member.
  - acceptance criteria: valid messages round-trip; an invalid key/version closes the session.
  - verification: `./gradlew test`

- [ ] **Discover peers with NSD in the foreground** (test-type: integration)
  - blocked-by: local protocol
  - summary: register, discover, resolve, and filter `_petit._tcp` with lifecycle and timeout.
  - desired behavior: peers are found without advertising indefinitely outside the app.
  - acceptance criteria: finds the other process, ignores itself, and releases listeners in `ON_STOP`.
  - verification: `./gradlew test`

- [ ] **Sync changesets over TCP idempotently** (test-type: both)
  - blocked-by: local protocol; NSD discovery; spec 0105
  - summary: exchange bidirectional batches, apply them in a transaction, and acknowledge with ACK.
  - desired behavior: retries and connection loss converge without duplicating or losing data.
  - acceptance criteria: two processes converge; a lost ACK allows a safe resend.
  - verification: `./gradlew test`

- [ ] **Schedule power-efficient background attempts** (test-type: integration)
  - blocked-by: TCP sync
  - summary: create unique periodic work with a connected network and backoff.
  - desired behavior: Android schedules attempts without a continuous service or persistent Wi-Fi Direct.
  - acceptance criteria: constraints, minimum periodicity, and uniqueness policy are verified.
  - verification: `./gradlew test`

- [ ] **Expose sync settings and state** (test-type: both)
  - blocked-by: TCP sync; periodic work
  - summary: implement on/off, a manual attempt, and an accessible global indicator.
  - desired behavior: the user understands and controls local syncing.
  - acceptance criteria: disabling stops NSD/the worker; states are localized and do not rely only on color.
  - verification: `./gradlew test`

- [ ] **Validate LAN sync on two devices** (test-type: integration)
  - blocked-by: settings and state
  - summary: test the same Wi-Fi network, bidirectional changes, reconnection, background, and an invalid key.
  - desired behavior: devices converge with the expected power usage and lifecycle.
  - acceptance criteria: all spec criteria pass on real hardware.
  - verification: `./gradlew assembleDebug && ./gradlew installDebug`
