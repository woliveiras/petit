# Plan: Google Account Authentication

Spec: [spec.md](./spec.md)

## Status

This plan is **On Hold**. It does not authorize implementation.

## Dependencies

- A validated Petit Cloud identity requirement.
- A selected Petit-managed identity backend.
- Approved account lifecycle and data-ownership behavior.

Google Drive backup is not a dependency and must not be implemented through
this authentication flow.

## Proposed sequence after revalidation

1. Define a provider-neutral `PetitIdentityRepository` and independent session states.
2. Add characterization tests proving local and Drive capabilities are account-free.
3. Implement one authentication provider behind the repository boundary.
4. Add account presentation, cancellation, sign-out, and recovery states.
5. Integrate entitlement resolution without deriving it from authentication alone.
6. Verify coexistence with an independently authorized Google Drive account.

## Constraints

- Do not add a Firebase dependency unless a later approved decision selects it.
- Do not reuse Google Drive scopes for Petit authentication.
- Do not revoke Drive access when signing out of Petit Cloud.
- Do not delete or hide local Room data because authentication is unavailable.

## Planned verification

- Focused repository and ViewModel unit tests.
- Provider-boundary integration tests.
- Instrumented authentication cancellation and account-switching tests.
- `./gradlew spotlessCheck`
- `./gradlew test`
