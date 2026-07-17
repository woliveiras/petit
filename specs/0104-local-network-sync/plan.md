# Plan: Local network sync

Spec: [spec.md](./spec.md)

## Starting point

There is no NSD/TCP implementation in the project. This plan should begin only
after explicit approval of the spec and assumes identity/key (0101), a local
group (0103), and deterministic conflict rules (0105).

## Implementation sequence

1. Define versioned `HELLO`, `HELLO_ACK`, `CHANGESET`, `ACK`, `ERROR`, and `CLOSE` messages.
2. Implement `NsdServiceManager` with register, discover, resolve, timeout, and filtering of its own service.
3. Implement a TCP server/client with authentication before the payload and size/time limits.
4. Create `LanSyncRepository` for changesets, ACK, and transactional integration with the resolver.
5. Integrate the lifecycle: start in `ON_START`, stop in `ON_STOP`, and clean up listeners/sockets.
6. Create unique periodic work with `NetworkType.CONNECTED`, backoff, and a minimum interval of 15 minutes.
7. Implement the on/off setting, manual action, and accessible global indicator.
8. Test in two processes and then on two devices on the same network.

## Protocol flow

1. The client sends `HELLO {protocolVersion, familyGroupKey, deviceId, lastSyncTimestamp}`.
2. The server validates the version, key, and member; an error closes the session.
3. The server responds with `HELLO_ACK {deviceId, lastSyncTimestamp}`.
4. Both exchange `CHANGESET` messages with entities newer than the known timestamp.
5. Each side applies the batch idempotently and responds with `ACK {newSyncTimestamp}`.
6. Both close the session; without an ACK, the batch can be safely resent.

## Battery and lifecycle

| Context | Behavior |
| --- | --- |
| Foreground | Active NSD and on-demand TCP. |
| Background | Periodic WorkManager, limited by constraints. |
| Terminated process | No persistent service. |
| Peer unavailable | Discovery ends after a timeout and retries later with backoff. |

Wi-Fi Direct is prohibited for continuous syncing. Nearby remains reserved for
pairing and one-off transfers.

## Risks and mitigation

| Risk | Mitigation |
| --- | --- |
| Discovery is slow or blocked by the network | Timeout, backoff, explicit state, and manual action. |
| TCP exposes data on the LAN | Authenticate before the payload and use a protected channel. |
| Simultaneous sessions duplicate work | Select the direction by IDs and keep application idempotent. |
| Background power consumption | Unique periodic work, constraints, and batching. |
| Divergent clocks | Do not treat the timestamp as sufficient for tie-breaking; apply spec 0105. |

## Final verification

1. Run protocol tests with two local processes and injected failures.
2. Run `./gradlew spotlessCheck` and `./gradlew test`.
3. Run `./gradlew assembleDebug && ./gradlew installDebug`.
4. On two devices, validate foreground, background, Wi-Fi loss/recovery, and an invalid key.
5. Confirm that NSD and sockets are released when leaving the app and that Wi-Fi Direct is not kept active.
