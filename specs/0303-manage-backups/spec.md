---
spec: "0303"
title: "Manage Google Drive Backups"
family: backup-recovery
status: Draft
owner: woliveiras
depends_on: ["0301"]
---

# Spec: Manage Google Drive Backups

## Context and motivation

The caregiver owns the backups stored in their Google Drive and needs complete
control over listing, inspecting, restoring, and permanently deleting them.
Petit does not impose backup-count, age, retention, or plan limits.

## Functional requirements

### Scenario 1: List every backup

```gherkin
GIVEN Google Drive is authorized
AND backups exist in appDataFolder
WHEN I open "Saved backups"
THEN Petit loads every result through pagination
AND sorts backups by creation time
AND shows date, trigger, app version, pet count, and archive size
```

### Scenario 2: Inspect backup details

```gherkin
GIVEN I select a backup
WHEN its details open
THEN I see archive and schema versions, creation time, trigger, content counts, size, and compatibility
AND I can choose Restore or Delete
AND no clinical values are exposed in provider metadata or logs
```

### Scenario 3: Delete one or multiple backups

```gherkin
GIVEN I select one or more backups
WHEN I choose Delete and confirm permanent deletion
THEN Petit permanently deletes only the selected Drive files
AND refreshes the list and total size
AND a retry treats already-missing selected files as deleted
```

### Scenario 4: Keep backups until the user deletes them

```gherkin
GIVEN backups of any age or count exist
WHEN Petit lists or creates backups
THEN Petit does not remove them automatically
AND no Petit Cloud plan changes their availability
AND provider quota errors are explained without deleting a backup
```

### Scenario 5: Disconnect without deleting

```gherkin
GIVEN Google Drive is authorized and contains backups
WHEN I disconnect or revoke Drive access
THEN Petit stops accessing Drive
AND remote backups remain in the user's Drive
AND reconnecting the same account makes them available again
```

### Scenario 6: Delete all backups explicitly

```gherkin
GIVEN one or more backups exist
WHEN I choose "Delete all backups" and confirm the destructive action
THEN Petit permanently deletes every backup created by this app
AND reports any files that could not be deleted
AND local data remains unchanged
```

### Scenario 7: Show empty and authorization-required states

```gherkin
GIVEN no backup is available or Drive needs authorization
WHEN I open Saved backups
THEN I see the correct empty or authorization-required state
AND I can create a backup or reconnect without a Petit account
```

## Non-functional requirements

- List only files recognized by the versioned Petit backup contract.
- Handle pagination and bounded parallel metadata reads.
- Minimize requested Drive fields and provider calls.
- Confirm every permanent deletion accessibly.
- Keep deletion idempotent and never delete by an unvalidated broad query.
- Never add automatic retention or a product-owned quota.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Filtering, pagination, sorting, totals, compatibility, selection, and deletion state. |
| Integration | Drive list/get/delete contracts, partial bulk failure, and ViewModels. |
| Instrumented | List, details, selection, destructive confirmation, empty, and reconnect UI. |
| Manual | Real appDataFolder listing, disconnect/reconnect, and permanent deletion. |

## Acceptance criteria

Every functional scenario requires traceable coverage before the status can
advance to `Implemented`.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Status | Draft | The updated behavior awaits explicit approval. |
| Retention | Until explicit user deletion | The user owns the storage and controls lifecycle. |
| Backup count | Unlimited by Petit | Only Google Drive quotas apply. |
| Disconnect | Preserve remote files | Revoking access is not a deletion request. |
| Account deletion purge | Removed | Drive backup does not depend on a Petit account or bucket. |

## Out of scope

- Automatically deleting backups because of age, count, billing, or account state.
- Managing non-Petit files.
- Moving or sharing appDataFolder files.
