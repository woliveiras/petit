# Plan: One-shot data transfer

Spec: [spec.md](./spec.md)

## Implementation status

Steps 1–6 are implemented and covered by local unit/Room tests. Step 7 remains
open because it requires two physical devices and the real Nearby radio path.

## Starting point

Serialization, Nearby payload callbacks, merge/replace options, and
presentation already exist. The pairing completion route does not currently
carry its connection into transfer, so the plan also connects those lifecycle
states before addressing replace semantics, atomicity, progress telemetry, and
hardware validation.

## Implementation sequence

1. Carry the authorized endpoint from successful pairing into an explicit send/receive flow.
2. Fix the list of entities included in the `ExportBundle` and validate the payload before writing.
3. Cover the current merge by UUID, `updatedAt`, and soft delete with tests.
4. Implement transactional replace: clear the shareable dataset and import the bundle.
5. Discard incomplete or cancelled transfers and map recoverable errors to the UI.
6. Derive progress from bytes actually sent/received and generate a result summary.
7. Validate merge, replace, repetition, cancellation, and interruption on two devices.

## Dependencies and integration

- Depends on the channel authorized by spec 0101.
- Uses Room as the local source and the existing export/import flow.
- Reuses the rules formalized in spec 0105.
- Technical reference: [local sharing protocols](../../docs/local-sharing-protocols.md).

## Risks and mitigation

| Risk | Mitigation |
| --- | --- |
| Replace deletes data before validating the bundle | Validate first and run cleanup/import in the same transaction. |
| Payload exceeds BYTES | Select FILE for large bundles and verify integrity at the end. |
| Repetition duplicates records | Make merge idempotent by UUID. |
| Connection drops during writing | Persist only after receiving and validating the complete payload. |

## Final verification

1. Run `./gradlew spotlessCheck` and `./gradlew test`.
2. Run `./gradlew assembleDebug && ./gradlew installDebug`.
3. On two devices, validate merge, replace, no internet connection, and interruption.
4. Compare the final database and displayed counters with the sent bundle.
