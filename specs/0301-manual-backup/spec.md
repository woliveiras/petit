---
spec: "0301"
title: "Manual Google Drive Backup"
family: backup-recovery
status: In Progress
owner: woliveiras
depends_on: ["0006", "0204"]
---

# Spec: Manual Google Drive Backup

## Context and motivation

The caregiver needs to create a complete portable backup in their own Google
Drive so that a different device can restore the app without requiring a Petit
account or a paid entitlement.

Petit requests Google Drive authorization only when a Drive capability is used.
The grant is independent from Petit Cloud authentication and uses the narrow
`drive.appdata` scope. Firebase is not part of this capability.

## Backup archive contract

Each backup is one ZIP archive stored in Google Drive `appDataFolder` and
contains:

```text
manifest.json
data/export.json
assets/pets/<pet-id>/<asset-file>
```

`manifest.json` includes:

- archive format and schema versions;
- backup ID, creation time, app version, and manual/automatic trigger;
- entry counts by data type;
- every payload entry's path, uncompressed size, media type, and SHA-256
  checksum; the manifest does not checksum itself;
- total payload count and total uncompressed payload size.

The final ZIP size and SHA-256 checksum are calculated after archive creation
and stored in the Drive file metadata and local completion record, avoiding a
self-referential manifest.

`data/export.json` contains a transactionally consistent snapshot of all
restorable user data: pets, weights, vaccinations, deworming records, tasks and
reminder state, user-visible app preferences, and references to included pet
assets. Soft-deleted rows required to reproduce the current state are included.

The archive includes the bytes for every app-owned pet asset instead of relying
on device-local URIs. It excludes Google credentials, Petit Cloud credentials,
device identity and pairing keys, family-group authorization, sync logs,
WorkManager bookkeeping, caches, and temporary files. Those values are
device-bound or security-sensitive and must be recreated safely after restore.

The archive is not encrypted by Petit. Google Drive provides storage and
transport protection; the UI and privacy documentation must state this
accurately without claiming end-to-end encryption.

## Functional requirements

### Scenario 1: Authorize Drive just in time

```gherkin
GIVEN I have no Petit account
AND Google Drive is not authorized
WHEN I choose "Back up to Google Drive"
THEN Petit explains the requested access
AND requests only the app-data authorization
WHEN I grant access
THEN the backup starts without creating a Petit account or requesting payment
```

### Scenario 2: Create a complete manual backup

```gherkin
GIVEN Google Drive is authorized
WHEN I choose "Back up now"
THEN Petit captures one consistent snapshot
AND builds a valid ZIP archive and manifest
AND uploads it to appDataFolder
AND shows monotonic byte progress and completion metadata
```

### Scenario 3: Include portable assets and preferences

```gherkin
GIVEN my data includes pet photos, tasks, reminder state, and app preferences
WHEN a backup completes
THEN the archive contains the restorable data and app-owned asset bytes
AND no archive entry depends on a URI that exists only on the source device
```

### Scenario 4: Preserve user control without Petit gates

```gherkin
GIVEN I already have any number of backups
WHEN I create another backup
THEN Petit creates it without deleting an older backup
AND no Petit plan, retention limit, or backup-count limit is applied
AND only Google Drive quotas can prevent the upload
```

### Scenario 5: Cancel or reject authorization safely

```gherkin
GIVEN I start authorization or backup
WHEN I cancel authorization or the operation
THEN no Petit account is created
AND no usable partial backup appears
AND local data remains unchanged
AND I can try again
```

### Scenario 6: Recover from transport and provider errors

```gherkin
GIVEN a backup upload is in progress
WHEN connectivity is lost, the token expires, or Drive rejects the request
THEN Petit classifies the error as retryable, authorization-required, quota, or permanent
AND retry cannot create duplicate completed backups for the same backup ID
AND incomplete upload state is resumed or discarded safely
```

## Non-functional requirements

- Preserve Room as the local source of truth.
- Generate the database snapshot inside one Room transaction.
- Enforce archive entry, total-size, uncompressed-size, path, and count limits.
- Use resumable upload and idempotent backup IDs.
- Never persist access or refresh tokens in the backup archive.
- Keep all visible states accessible and localized.
- Keep local export/import available without Google Play Services or a network.
- Do not add client-side encryption unless a later approved spec changes the
  security model.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | ZIP manifest, checksums, limits, snapshot mapping, state, and error classification. |
| Integration | Room snapshot, asset packaging, Drive gateway contract, resumable retry, and ViewModel. |
| Instrumented | Google authorization result handling and Settings flow. |
| Manual | Real account consent, revocation, quota, interruption, and physical-device upload. |

## Acceptance criteria

Every functional scenario and archive-contract rule requires traceable coverage
before the status can advance to `Implemented`.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Status | Approved | The behavior is approved for implementation. |
| Storage | Google Drive `appDataFolder` | User-owned storage with the narrow app-data scope. |
| Petit account | Not required | Drive authorization is independent from Petit Cloud. |
| Price | Free | Petit does not operate the storage infrastructure. |
| Archive | Versioned ZIP with manifest and assets | A JSON file containing local URIs is not portable. |
| Client encryption | None | The product relies on Google Drive security. |
| Retention and count | User-controlled, no Petit limit | Petit must not delete backups merely to enforce a product gate. |

## Out of scope

- Petit Cloud synchronization or hosted storage.
- Sharing a backup with another Google account.
- Automatically deleting old backups.
- Restoring an archive; owned by spec 0302.
- Listing and deletion UI; owned by spec 0303.
