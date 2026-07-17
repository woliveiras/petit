# Plan: JSON export and import

Spec: [spec.md](./spec.md)

## Sequence

1. Aggregate the five domains into `ExportBundle` with `ExportMetadata`.
2. Serialize/deserialize with `org.json` and version the schema.
3. Read and write through `ContentResolver`, validating the document before import.
4. Analyze counts and conflicts and apply `REPLACE`, `KEEP`, or `MERGE` in a transaction.
5. Integrate export/import with settings and per-pet export with the profile.

## Architecture

- `ExportImportUseCase` coordinates repositories, parsing, analysis, and merge.
- `ExportBundle` uses the keys `pets`, `weightEntries`, `vaccinationEntries`, `dewormingEntries`, and `tasks`.
- The import is persisted only after validation and user confirmation.

## Dependencies and risks

- Depends on `0001`–`0005` and must preserve IDs and references.
- Schema changes require explicit migration and tests with older backups.
- I/O or parsing failures must not produce a partial import.
