---
spec: "0201"
title: "Google Account Authentication"
family: identity-access
status: On Hold
owner: woliveiras
depends_on: []
---

# Spec: Google Account Authentication

## Context and motivation

Petit may eventually use a Google account as one authentication option for
Petit Cloud. This capability identifies a person to Petit-managed services; it
does not grant Google Drive access and is not required for user-owned backup.

Google Drive backup uses a separate, just-in-time Google authorization owned by
spec 0301. A user can authorize Drive without creating or authenticating a
Petit account.

## Functional requirements

### Scenario 1: Authenticate to Petit Cloud with Google

```gherkin
GIVEN Petit Cloud is available
AND I am not authenticated to Petit Cloud
WHEN I choose "Continue with Google"
AND complete the Google account flow
THEN Petit establishes a Petit Cloud identity
AND the UI identifies the session as "Petit Cloud"
```

### Scenario 2: Continue using Petit without authentication

```gherkin
GIVEN I do not have a Petit Cloud account
WHEN I use Petit Local or a user-owned Google Drive capability
THEN no Petit authentication is required
AND all local data remains available
```

### Scenario 3: Keep Drive authorization independent

```gherkin
GIVEN Google Drive is authorized
AND I am not authenticated to Petit Cloud
WHEN I create, restore, or manage a Drive backup
THEN the capability remains available
AND Drive authorization does not create a Petit account
AND Drive authorization does not imply a paid entitlement
```

### Scenario 4: Sign out without affecting local data or Drive

```gherkin
GIVEN I am authenticated to Petit Cloud
AND Google Drive is independently authorized
WHEN I sign out of Petit Cloud
THEN my local data remains available
AND Google Drive remains authorized until I explicitly disconnect or Google revokes access
```

### Scenario 5: Handle cancellation and authentication failure

```gherkin
GIVEN I start Petit Cloud authentication
WHEN I cancel or the provider fails
THEN I remain unauthenticated
AND local and Google Drive capabilities continue to work according to their own state
AND I can try again
```

## Non-functional requirements

- Preserve offline and account-free use of Petit Local.
- Keep Petit authentication, Google Drive authorization, and Petit Cloud
  entitlement as three independent states.
- Store no long-lived Google credential directly in app storage.
- Provide accessible loading, cancellation, success, and error states.
- Do not make Firebase or any specific identity backend a product requirement.

## Test strategy

| Scope | Expected coverage |
| --- | --- |
| Unit | State separation, cancellation, sign-out, and entitlement independence. |
| Integration | Identity provider boundary and coexistence with Drive authorization. |
| Manual | Provider account picker, cancellation, revocation, and account switching. |

## Acceptance criteria

The functional scenarios define the testable boundary. They require traceable
coverage before this spec can advance after being resumed and approved.

## Decisions

| Decision | Current choice | Rationale |
| --- | --- | --- |
| Proposal status | On Hold | Petit Cloud identity is not required for the current local or user-owned cloud roadmap. |
| Google Drive relationship | Separate authorization owned by 0301 | Authentication and authorization are different grants. |
| Firebase | Not selected | The eventual Petit Cloud identity backend remains an implementation decision. |
| Local source of truth | Room | Identity availability must not compromise local access. |

## Out of scope

- Google Drive authorization, backup, restore, or backup management.
- Treating Google authentication as proof of purchase.
- Selecting or implementing a Petit Cloud identity backend before this spec is
  revalidated and approved.
