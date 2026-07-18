---
spec: "0305"
title: "Automatic Google Drive Backup"
family: backup-recovery
status: Approved
owner: woliveiras
depends_on: ["0301"]
---

# Spec: Automatic Google Drive Backup

## Context and motivation

The caregiver can opt into free automatic backups in their own Google Drive.
Android WorkManager runs durable, deferrable work when system and network
constraints permit; Petit does not promise an exact clock time.

## Functional requirements

### Scenario 1: Opt in explicitly

```gherkin
GIVEN Google Drive is authorized
AND automatic backup is off by default
WHEN I enable automatic backup
THEN Petit enqueues one unique daily periodic work request
AND explains that Android chooses the exact execution time
AND no Petit account or entitlement is required
```

### Scenario 2: Run while the app is closed

```gherkin
GIVEN automatic backup is enabled
AND all configured constraints are satisfied
WHEN WorkManager runs the periodic request
THEN Petit creates and uploads the current complete archive
AND records the attempt even if the app process was previously closed
```

### Scenario 3: Respect network constraints

```gherkin
GIVEN automatic backup requires an unmetered network
WHEN only a metered network is available
THEN WorkManager defers the backup
WHEN an unmetered network becomes available
THEN the work becomes eligible to run
```

### Scenario 4: Handle revoked authorization in background

```gherkin
GIVEN a scheduled backup starts
AND Google requires user interaction to authorize again
WHEN the worker checks authorization
THEN it does not launch UI from the background
AND records "Authorization required"
AND offers a foreground reconnect action through Settings or a notification
```

### Scenario 5: Retry transient failures

```gherkin
GIVEN a scheduled backup encounters a transient network or provider failure
WHEN the worker returns retry
THEN WorkManager applies exponential backoff
AND retry reuses the same idempotent operation where possible
AND no duplicate completed backup is created
```

### Scenario 6: Preserve every completed backup

```gherkin
GIVEN automatic backups complete repeatedly
WHEN a new backup is created
THEN Petit does not delete older automatic or manual backups
AND only an explicit user deletion removes a completed backup
```

## Scheduling rules

- Use `enqueueUniquePeriodicWork` for one daily request.
- Treat the repeat interval as a minimum interval, not an exact appointment.
- Display "Scheduled" or an execution window rather than an exact next time.
- Use WorkManager constraints for connected or unmetered network, battery not
  low, and storage not low as approved by the settings spec.
- Use exponential backoff for transient errors.
- Do not use exact alarms for backup.
- Cancel unique periodic work when automatic backup is disabled.

## Non-functional requirements

- Reuse the exact archive and Drive upload path from spec 0301.
- Keep automatic backup free and independent from Petit authentication.
- Never prompt for authorization from a Worker.
- Avoid persistent services and unnecessary wakeups.
- Keep work unique, observable, cancelable, and idempotent.
- Do not apply automatic retention or count limits.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | Scheduling policy, constraints, backoff mapping, authorization state, and idempotency. |
| Integration | WorkManager, Hilt worker, archive creation, Drive gateway, and history. |
| Instrumented | Settings opt-in/out and foreground reauthorization. |
| Manual | Real background execution, process death, Doze, network changes, and revocation. |

## Acceptance criteria

Every functional scenario and scheduling rule requires traceable coverage
before the status can advance to `Implemented`.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Status | Approved | The behavior is approved for implementation. |
| Price | Free | Storage is user-owned. |
| Default | Off | Automatic remote transfer requires explicit opt-in. |
| Schedule | Daily, inexact WorkManager execution | Android does not guarantee an exact 02:00 run. |
| Retention | No automatic deletion | Users control their backups. |
| Background authorization | No UI; report reauthorization required | Background work cannot complete interactive consent. |

## Out of scope

- Exact-time alarms.
- Petit Cloud backup or synchronization.
- Automatic deletion based on age or count.
- Change-triggered debounce; owned by spec 0307.
