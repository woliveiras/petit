---
spec: "0306"
title: "Backup Settings"
family: backup-recovery
status: Approved
owner: woliveiras
depends_on: ["0305"]
---

# Spec: Backup Settings

## Context and motivation

The caregiver needs clear control over Google Drive connection, automatic
backup, network constraints, notifications, manual execution, and history. No
setting is gated by a Petit account or payment.

## Functional requirements

### Scenario 1: See independent service state

```gherkin
GIVEN I open Backup settings
THEN I see whether Google Drive is disconnected, authorizing, connected, needs authorization, or unavailable
AND I do not see the state described as Petit Cloud login or premium
```

### Scenario 2: Enable and disable automatic backup

```gherkin
GIVEN Google Drive is authorized
WHEN I enable automatic backup
THEN one daily periodic request is scheduled
AND the UI says Android runs it when conditions permit
WHEN I disable automatic backup
THEN the periodic request is canceled
AND manual backup remains available
```

### Scenario 3: Configure network usage

```gherkin
GIVEN automatic backup is enabled
WHEN I enable "Unmetered networks only"
THEN WorkManager requires NetworkType.UNMETERED
WHEN I disable it
THEN WorkManager requires NetworkType.CONNECTED
AND the unique schedule is updated without duplication
```

### Scenario 4: Configure completion notifications

```gherkin
GIVEN I can change "Notify after backup"
WHEN it is enabled and an automatic backup succeeds
THEN I receive a silent localized notification with non-clinical counts
WHEN it is disabled
THEN success produces no notification
AND authorization-required and actionable failure notifications remain permitted
```

### Scenario 5: Back up now independently

```gherkin
GIVEN Google Drive is authorized
WHEN I choose "Back up now"
THEN a manual backup begins immediately through the shared backup use case
AND it does not reset, duplicate, or replace the periodic schedule
```

### Scenario 6: View accurate history and schedule state

```gherkin
GIVEN backup attempts exist
WHEN I open Backup settings or history
THEN I see trigger, start time, completion time, status, size, and non-clinical counts
AND scheduled work is described as inexact
AND failures provide an actionable category without sensitive data
```

### Scenario 7: Disconnect Google Drive

```gherkin
GIVEN Google Drive is connected
WHEN I disconnect and confirm
THEN Drive access is revoked
AND automatic work is canceled
AND local data and remote backups are not deleted
AND I can reconnect later
```

## Settings model

- Google Drive connection state.
- Automatic backup enabled, default `false`.
- Network requirement: connected or unmetered, default unmetered.
- Notify after successful automatic backup, default `false`.
- Last attempt and last success summaries.

The settings model does not contain a retention period, maximum backup count,
premium flag, exact run time, or provider credential.

## Non-functional requirements

- Persist preferences in DataStore.
- Update unique WorkManager requests atomically with preference changes.
- Localize all visible copy and content descriptions.
- Do not show an exact future execution time WorkManager cannot guarantee.
- Keep actionable errors distinct from transient background retries.
- Never expose pet names, clinical values, access tokens, or file contents in history or notifications.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Defaults, persistence mapping, state copy, notification policy, and schedule descriptions. |
| Integration | DataStore, WorkManager update/cancel, history, manual action, and disconnect. |
| Instrumented | Settings accessibility, authorization launcher, toggles, history, and confirmations. |

## Acceptance criteria

Every functional scenario and settings-model rule requires traceable coverage
before the status can advance to `Implemented`.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Status | Approved | The behavior is approved for implementation. |
| Automatic default | Off | Remote automation requires explicit opt-in. |
| Network default | Unmetered | Reduces unexpected mobile-data use. |
| Schedule presentation | Inexact | WorkManager does not promise a precise execution time. |
| Manual backup | Always available with Drive authorization | Automation settings must not gate basic backup. |
| Retention controls | None | Petit does not delete user-owned backups automatically. |

## Out of scope

- Selecting exact clock times.
- Configuring automatic retention or backup-count limits.
- Petit Cloud billing or entitlement settings.
