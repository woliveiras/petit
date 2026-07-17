# Plan: Weight tracking

Spec: [spec.md](./spec.md)

## Sequence

1. Model `WeightEntryEntity` with a reference to `PetEntity` and store `weightGrams`.
2. Implement queries by pet/date, latest weight, sorting, and upsert.
3. Implement conversion, validation, editing, and soft delete in the repository/ViewModel.
4. Integrate the form, history, and Vico bar chart.

## Architecture

- `weight_entries` uses `petId`, synchronization metadata, and an active query.
- The pet/date combination determines upsert behavior.
- The chart uses Vico with vertical bars; the presentation layer converts grams to kg.

## Dependencies and risks

- Depends on `0001` for the relationship with the pet.
- Dates must be normalized to prevent duplicates caused by time zones.
