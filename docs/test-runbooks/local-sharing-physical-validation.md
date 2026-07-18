# Runbook: local sharing physical validation

## Document control

| Field | Value |
| --- | --- |
| Family | `local-sharing` |
| Specs | 0101, 0102, 0103, 0104, and 0105 |
| Type | Manual test on two physical Android devices |
| Expected initial status | Specs in `Implemented`; physical tasks open |
| Allowed result | `Pass`, `Fail`, or `Blocked` |

## Objective

Validate on real hardware the behaviors that cannot be proven solely with local
tests, an emulator, or two processes: Nearby Connections, real permissions and
radios, lack of internet access, NSD on a real Wi-Fi network, transport
interruptions, background execution, and convergence between two independent
databases.

This runbook does not replace `./gradlew spotlessCheck`, `./gradlew test`, or
instrumented tests. Run it only against a commit that has already passed those
checks.

## Completion criteria

The family may move from `Implemented` to `Completed` only when:

1. all mandatory cases in this runbook have a `Pass` result;
2. no open defect invalidates an acceptance criterion;
3. the evidence identifies the commit, APK, devices, and result;
4. the physical tasks for 0101–0105 are marked as completed;
5. the corresponding physical criteria in the specs are reconciled.

Do not use `Pass` for a step that was not executed. Use `Blocked` when the
environment does not allow execution, and record the reason.

## Responsibilities

- **Executor:** operates the devices and records evidence without changing the flow.
- **Observer:** verifies results on both devices and records times, counters, and
  discrepancies.
- One person may perform both roles, but the checks must cover both sides.

## Preconditions

- Two physical Android devices, referred to as **A** and **B**.
- Android versions and device models recorded in the execution worksheet.
- The same debug APK, built from the same commit, installed on both devices.
- Cable/ADB or wireless debugging available for log collection.
- Bluetooth and Wi-Fi working on both devices.
- A local Wi-Fi network on which clients can communicate with each other.
- The ability to block internet access without disabling the local network.
- Android Studio Database Inspector available for the exact-tie case.
- Test data must not contain real clinical information.

## Build preparation

```bash
git status --short
git rev-parse --short HEAD
./gradlew spotlessCheck
./gradlew test
./gradlew assembleDebug && ./gradlew installDebug
```

The APK is located at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install the same file on A and B. If `installDebug` selects only one device,
install it explicitly on the other device:

```bash
adb -s <serial-A> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial-B> install -r app/build/outputs/apk/debug/app-debug.apk
```

## Records and evidence

Create a folder outside the repository or ignored by Git:

```text
local-sharing-<commit>-<YYYY-MM-DD-HHMM>/
```

For each case, record:

- result (`Pass`, `Fail`, or `Blocked`);
- start and end times;
- screenshots or recordings from both devices;
- relevant logcat output, without keys or sensitive data;
- displayed state and counters;
- notes and the bugfix ID, if the case fails.

Useful commands:

```bash
adb -s <serial-A> logcat -c
adb -s <serial-B> logcat -c
adb -s <serial-A> logcat -v threadtime > device-A.log
adb -s <serial-B> logcat -v threadtime > device-B.log
```

## Fixtures

After clearing app data on both devices, create:

| Device | Pet | Unique record | Device name |
| --- | --- | --- | --- |
| A | `Mimi-A` | weight identifiable as `A` | `Petit A` |
| B | `Mimi-B` | task identifiable as `B` | `Petit B` |

Record the initial number of pets, weights, vaccines, deworming treatments, and
tasks on each device. These values will be used for the MERGE and REPLACE
counters.

## Test cases

### LS-PAIR-01 — incorrect code and successful pairing

**Covers:** 0101; authorization, retry, and post-authorization persistence.

1. On A, start the mode that advertises the four-digit code.
2. On B, start discovery and select A.
3. Enter a code different from the one displayed on A.
4. Confirm that B shows a rejection and allows another attempt.
5. Confirm that no incomplete group appears on A or B.
6. Enter the correct code.
7. Confirm pairing on both devices.
8. Close and reopen the app on both devices.

**Expected result:** the incorrect code neither authorizes nor persists an
association; the correct code persists the same group and stable identities on
both sides.

**Minimum evidence:** a video showing the incorrect code, retry, success, and
state after restart.

### LS-PAIR-02 — cancellation and endpoint loss

**Covers:** 0101; idempotent cleanup.

1. Start advertising on A and discovery on B.
2. Cancel on B before entering the code.
3. Repeat, then disable Bluetooth or Wi-Fi on A while B is waiting.
4. Restore the radios and repeat pairing from the beginning.

**Expected result:** advertising, discovery, and connection are stopped without
a partial group; a new attempt works without restarting the device.

### LS-PAIR-03 — pairing without internet access

**Covers:** 0101; local operation without a remote server.

1. Keep Bluetooth and the local Wi-Fi network enabled.
2. Block mobile internet and WAN access on both devices.
3. Use a browser to confirm there is no internet access.
4. Run LS-PAIR-01 with the correct code.

**Expected result:** pairing completes normally without internet access.

### LS-XFER-01 — authorized MERGE

**Covers:** 0102 and 0105; transfer, merge, and counters.

1. Use a paired group and keep different fixtures on A and B.
2. Start the A → B transfer.
3. On B, select MERGE.
4. Observe progress until it reaches 100%.
5. Compare B's visible database with the fixtures.
6. Verify added, updated, and removed counters by type.

**Expected result:** B retains its data and receives A's data; progress is
monotonic, and the counters match the recorded differences.

### LS-XFER-02 — destructive REPLACE

**Covers:** 0102; confirmation and exact replacement.

1. On B, create a record that does not exist on A.
2. Start A → B and select REPLACE.
3. Cancel the first confirmation and verify that nothing changed.
4. Repeat and confirm the destructive action.
5. Compare all entity types on A and B.

**Expected result:** nothing changes without confirmation; after confirmation,
B exactly reflects A's bundle and removes missing records, with correct
counters.

### LS-XFER-03 — interruption, cancellation, and retry

**Covers:** 0102; partial payload and recovery.

1. Prepare enough data to make progress visible.
2. Start A → B.
3. During the transfer, disable Bluetooth/Wi-Fi on one device.
4. Confirm that B did not persist a partial payload.
5. Restore the radios and run the transfer again until it completes.
6. Repeat using the interface's cancel button.
7. Repeat without internet access.

**Expected result:** interruption and cancellation do not change the database;
retry completes without duplication; internet access is not required.

### LS-GROUP-01 — rename and last sync

**Covers:** 0103; stable identity and UI states.

1. Record A's UUID, key/group, and name.
2. Rename A to `Petit A Renamed`.
3. Confirm the new name locally and restart the app.
4. Sync with B.
5. Confirm the new name on B and the last-sync time on both devices.

**Expected result:** only the name changes; UUID, group, and authorization remain.

### LS-GROUP-02 — removal and revocation of the old key

**Covers:** 0103; idempotent removal and revocation.

1. On A, remove B and confirm the action.
2. Sync or allow the event to propagate.
3. On B, try to start a sync using the old association.
4. Repeat the removal on A if the interface still allows it.
5. Verify that pets remain on both devices.

**Expected result:** B disappears from the association and cannot access data
with the old key; repeating the operation has no additional effects, and
clinical data remains intact.

### LS-GROUP-03 — offline leave and new group

**Covers:** 0103 and 0104; leave outbox and multi-group isolation.

1. Pair A and B again if necessary.
2. Take A offline and make A leave the group.
3. Confirm that the visible key is removed and A's pets remain.
4. On A, join a new group with a third peer or recreate the association on B.
5. Restore the old network and run manual attempts until the `LEAVE` is delivered.
6. Confirm that the current group continues syncing even if the old peer is
   unavailable.
7. Restart A and repeat an attempt.

**Expected result:** the old `LEAVE` is delivered exactly once, does not leak
clinical data, does not block the current group, and its restricted credential
is discarded after ACK.

### LS-CONFLICT-01 — edit/edit

**Covers:** 0105; determinism and symmetry.

1. Sync a baseline record to both devices.
2. Disable sync and disconnect the network.
3. Edit the same record with different values on A and B, recording the order.
4. Reconnect and sync in both directions.
5. Repeat the sync without making new changes.

**Expected result:** both devices end with the truly most recent version; retry
does not change state or increment counters again.

### LS-CONFLICT-02 — edit/delete

1. Sync a baseline record.
2. While offline, edit it on A and delete it on B.
3. Run once with the edit being newer, and once with the deletion being newer.
4. Sync in both directions.

**Expected result:** the operation with the genuinely newer timestamp wins;
both devices converge without improperly resurrecting the tombstone.

### LS-CONFLICT-03 — delete/delete

1. Sync a baseline record.
2. While offline, delete it on A and B.
3. Reconnect, sync in both directions, and repeat.

**Expected result:** both devices retain a convergent and idempotent deleted
result, without duplicate history entries.

### LS-CONFLICT-04 — exact timestamp tie

1. Start from a synced baseline record.
2. While offline, create divergent payloads on A and B.
3. In Database Inspector, assign the same `updatedAt` to both versions. Record
   the value used and export evidence of the rows, without sensitive content.
4. Sync A → B and B → A.
5. Repeat the order from restored databases, reversing the first transfer.
6. Run a retry without making new changes.

**Expected result:** the winner is the same in both orders, according to the
documented tie-breaker; retry is idempotent.

### LS-LAN-01 — discovery and bidirectional foreground sync

**Covers:** 0104; NSD, authentication, and bidirectional changesets.

1. Put A and B on the same Wi-Fi network with client isolation disabled.
2. Enable automatic sync on both devices.
3. Create a unique change on each device.
4. Open both apps in the foreground.
5. Observe discovery, sync, and completion states.
6. Verify data, history, peer, time, type, and counters on both devices.
7. Leave the app and verify in the log that discovery and sockets were released.

**Expected result:** both devices converge; no clinical data is sent before
authentication; the lifecycle releases resources outside the foreground.

### LS-LAN-02 — Wi-Fi loss and return

1. Start a session with pending changes on both devices.
2. Disable Wi-Fi during the transfer.
3. Confirm an error/peer-unavailable state and no partial persistence.
4. Re-enable Wi-Fi without creating a new association.
5. Wait or trigger a manual attempt.

**Expected result:** accumulated changes converge without duplication, and a
lost ACK can be retried safely.

### LS-LAN-03 — background execution and WorkManager

1. Enable automatic sync and leave changes pending.
2. Put the apps in the background.
3. Keep the devices charged and connected to the network for the interval
   allowed by Android, never less than the configured 15 minutes.
4. Record WorkManager state using `adb shell dumpsys jobscheduler` and logs.
5. Reopen the apps and verify data and history.

**Expected result:** there is one unique periodic job, with a connected-network
constraint and backoff; there is no persistent service or continuous Wi-Fi
Direct group.

### LS-LAN-04 — toggle off and manual attempt

1. Disable automatic sync on A.
2. Reopen A and wait beyond the normal timeout.
3. Confirm through the UI/log that A does not advertise or discover automatically.
4. Create a change and use the manual-attempt action.
5. Re-enable the toggle and repeat in the foreground.

**Expected result:** the toggle stops automatic NSD and worker activity; the
manual action starts a new attempt and does not reuse a stale `Synced` state.

### LS-LAN-05 — invalid key before payload

This case requires a debug build and a debugger because the normal flow filters
different groups during NSD.

1. Keep A and B in the same group and start discovery.
2. On B, pause at the beginning of `LanSessionRunner.runClient`, after the peer
   has been resolved and before `LanHandshakeClient` is created.
3. For this session only, use the debugger to replace the handshake key with
   another valid 32-byte Base64URL key, preserving the advertised group ID. Do
   not persist the changed key.
4. Resume execution and capture logs from both devices.
5. Confirm that A returns an error and closes before receiving `CHANGESET`.
6. Remove the debugger change and confirm that a normal attempt succeeds.

**Expected result:** `AUTHENTICATION_FAILED`/`ERROR` and `CLOSE` occur before
any clinical payload; no database or persistent credential is changed.

If the debugger cannot inject the key without changing the APK, record
`Blocked` and use the automated invalid-handshake test as supporting evidence;
do not mark the physical validation as `Pass`.

## Final regression

After all cases:

1. restart A and B;
2. confirm that the correct active group remains available;
3. confirm that pets and local history were not deleted by remove/leave;
4. run another sync with no changes and confirm there is no duplication;
5. run `./gradlew spotlessCheck` and `./gradlew test` again on the tested commit;
6. check `git status --short` before updating specs and tasks.

## Failure handling

If a result is `Fail` and the behavior is not described as pending in the spec:

1. preserve logs, videos, commit, devices, and reproduction steps;
2. create `docs/bugfixes/YYYY-MM-DD-<slug>.md`;
3. document reproduction, impact, hypothesis, regression, and proposal;
4. respect the bugfix approval gate before changing production code;
5. after the fix, repeat the failed case and the final regression.

## Execution worksheet

```text
Date/time:
Executor:
Observer:
Commit:
APK SHA-256:
Device A / Android / serial:
Device B / Android / serial:
Network / router / client isolation:
Internet available at the start: yes / no

LS-PAIR-01: Pass / Fail / Blocked — evidence:
LS-PAIR-02: Pass / Fail / Blocked — evidence:
LS-PAIR-03: Pass / Fail / Blocked — evidence:
LS-XFER-01: Pass / Fail / Blocked — evidence:
LS-XFER-02: Pass / Fail / Blocked — evidence:
LS-XFER-03: Pass / Fail / Blocked — evidence:
LS-GROUP-01: Pass / Fail / Blocked — evidence:
LS-GROUP-02: Pass / Fail / Blocked — evidence:
LS-GROUP-03: Pass / Fail / Blocked — evidence:
LS-CONFLICT-01: Pass / Fail / Blocked — evidence:
LS-CONFLICT-02: Pass / Fail / Blocked — evidence:
LS-CONFLICT-03: Pass / Fail / Blocked — evidence:
LS-CONFLICT-04: Pass / Fail / Blocked — evidence:
LS-LAN-01: Pass / Fail / Blocked — evidence:
LS-LAN-02: Pass / Fail / Blocked — evidence:
LS-LAN-03: Pass / Fail / Blocked — evidence:
LS-LAN-04: Pass / Fail / Blocked — evidence:
LS-LAN-05: Pass / Fail / Blocked — evidence:

Overall result: Pass / Fail / Blocked
Open bugfixes:
Notes:
```
