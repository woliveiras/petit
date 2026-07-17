# Plan: Vaccination records

Spec: [spec.md](./spec.md)

## Sequence

1. Model `VaccinationEntryEntity` with the pet key and audit metadata.
2. Implement the repository, active queries, and `HealthStatus` calculation.
3. Implement the form with a catalog filtered by `PetType` and a custom type.
4. Integrate grouping, visual indicators, editing, and deletion.

## Architecture

- Room stores the doses; the domain calculates status from `nextDueDate` and the current date.
- `VaccinationViewModel` accesses the repository and exposes state to `VaccinationFormScreen` and `VaccinationRecordsScreen`.
- Saving or deleting a dose may trigger automated tasks from spec `0005`.

## Dependencies and risks

- Depends on `0001`; optionally integrates with `0005`.
- Day calculation must be deterministic and testable with a controlled clock.
