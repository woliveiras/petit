---
spec: "0102"
title: One-shot data transfer
family: local-sharing
status: Implemented
owner: woliveiras
depends_on: ["0101"]
---

# Spec: One-shot data transfer

## Context and motivation

After pairing, a caregiver needs to send the pets' complete history to another
device without a server or internet connection. The receiver chooses whether
to merge the received data or replace its local database.

## Current state

`NearbyTransferRepository`, `SendDataUseCase`, `MergeDataUseCase`, and the
transfer interface already exist. However, completing pairing returns to
Settings, while the transfer routes later expect the process-local Nearby
connection to still be available; the app does not hand the paired connection
into a transfer automatically. `REPLACE` mode overwrites matching entities but
does not remove local records missing from the bundle. Sending reports only an
initial zero-byte state, receiving progress depends on already-buffered data,
and success has not been validated on two devices.

## Requirements

### Functional

- [x] Serialize shareable data into an `ExportBundle`.
- [x] Carry an authorized Nearby connection from pairing into send/receive.
- [x] Serialize and submit a bundle when a connected endpoint is already available.
- [x] Offer the receiver merge and replace options.
- [x] Merge entities by UUID and `updatedAt`.
- [x] Make `REPLACE` remove missing local data before import.
- [x] Display actual progress and a summary by entity type.
- [x] Discard a partial payload when the transfer fails.
- [x] Confirm the destructive mode before replacing data.
- [x] Allow cancellation to disconnect and discard any partial payload.

### Non-functional

- [x] Integrity: apply the import atomically.
- [x] Security: accept payloads only from the paired endpoint.
- [x] Performance: choose BYTES or FILE based on the payload limit without truncating the bundle.
- [x] Accessibility and i18n: announce progress and keep text in pt-BR, en, and es.

## Test strategy

Unit tests cover serialization, merge, replace, counters, and incomplete
payload handling. Integration tests cover Room, Nearby, and the flow between
two devices, including without an internet connection. See the
[protocol research](../../docs/local-sharing-protocols.md).

## Acceptance criteria

- [ ] Given a paired device with local data, when it sends the data, then the receiver gets a complete `ExportBundle` and sees progress and completion.
- [x] Given a receiver with existing data, when it chooses merge, then UUIDs are matched and the version with the latest `updatedAt` prevails.
- [x] Given a receiver with records missing from the bundle, when it confirms replacement, then the shareable database is cleared and reflects only the bundle.
- [ ] Given an interrupted connection, when only part of the payload arrives, then no changes are persisted and another attempt is offered.
- [ ] Given two devices without an internet connection, when they transfer through Nearby, then the flow succeeds.
- [x] Given a completed import, when the summary is displayed, then the counters match the entities actually added, updated, and removed.

## Edge cases

- Empty or incompatible bundle, or one larger than the BYTES limit.
- Insufficient space or transactional failure on the receiver.
- Child entities whose pet does not exist in the bundle.
- Same bundle received more than once.
- Soft delete newer than the active copy.

## Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Format | Serialized `ExportBundle` | Reuses the existing local export/import boundary. |
| Merge | UUID + `updatedAt` | Keeps the result deterministic for versions with different timestamps. |
| Replacement | Transactional cleanup followed by import | Makes the behavior match the meaning presented to the user. |
| Transport | Nearby after spec 0101 | Works locally and reuses the authorized channel. |

## Out of scope

- Discovering or pairing devices.
- Automatic or incremental synchronization.
- Resolving ties between identical timestamps; see spec 0105.
- Cloud backup.
