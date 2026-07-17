# Plan: Deworming records

Spec: [spec.md](./spec.md)

## Sequence

1. Model `DewormingEntryEntity` with a reference to the pet.
2. Implement the repository, active-record queries, and per-record calculation.
3. Integrate the form, history, editing, and soft delete.
4. Extend queries to retrieve the latest record by category and count `BOTH`.
5. Display internal and external sections with aggregate status.

## Architecture

- Room stores `type`, medication, dates, and synchronization metadata.
- `DewormingViewModel` accesses the repository and exposes state to the form and records screens.
- The category view requires selecting the latest applicable records for each category.
- Saving or deleting may trigger automatic tasks from spec `0005`.

## Dependencies and risks

- Depends on `0001`; optionally integrates with `0005`.
- `BOTH` may compete with category-specific records; the rule must choose the latest applicable event.
