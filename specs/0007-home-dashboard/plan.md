# Plan: Home dashboard

Spec: [spec.md](./spec.md)

## Sequence

1. Aggregate pets, tasks, timeline, and latest weight in `HomeViewModel`.
2. Calculate `PetWithSummary` and the empty state without blocking the UI.
3. Implement cards, upcoming tasks, recent activity, and pull-to-refresh.
4. Integrate navigation to the profile, full lists, settings, and Quick Add.
5. Evaluate visual separation of alerts and the overall healthy state.

## Architecture

- `HomeViewModel` combines `PetRepository`, `TaskRepository`, `TimelineRepository`, `WeightEntryRepository`, and the health summary.
- `HomeUiState` represents loading, refresh, pets, tasks, and events.
- `QuickAddScreen` provides five actions; `PetSelectionScreen` mediates actions that require a pet.
- `ActivityTimelineScreen` provides filters by period and pet.

## Dependencies and risks

- Depends on `0001`–`0005`; partial failures from one source must not hide the entire dashboard.
- Per-pet aggregations must avoid excessive queries and inconsistent states.
