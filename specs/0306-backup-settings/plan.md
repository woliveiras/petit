# Plan: Backup Settings

Spec: [spec.md](./spec.md)

## Status

This plan is **Approved** and may be implemented.

## Dependencies

- Spec 0305 scheduler, worker, attempt history, and reauthorization state.
- Spec 0301 manual backup and Drive disconnect contracts.

## Implementation sequence

1. Extend DataStore with defaults for automatic, network, and notification settings.
2. Expose independent Drive connection and backup status in a dedicated ViewModel.
3. Implement atomic enable, disable, and network-constraint schedule updates.
4. Add manual backup without changing the periodic schedule.
5. Add success notification policy and actionable authorization/failure notifications.
6. Add history and inexact schedule presentation.
7. Add explicit disconnect confirmation that preserves all data.
8. Verify localization and accessibility in every supported locale.

## Planned verification

- DataStore repository integration tests.
- WorkManager unique update/cancel tests.
- ViewModel state and event tests.
- Compose UI and accessibility tests.
- Physical authorization/disconnect verification.
- `./gradlew spotlessCheck`
- `./gradlew test`
