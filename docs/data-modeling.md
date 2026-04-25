# Petit Data Modeling

This document describes the current data model for Petit.

## Overview

Petit follows an offline-first model with Room as the local source of truth. The central entity is `Pet`, and health records reference it. A `Pet` has a `petType` field that determines which health records and vaccine types are applicable.

## Core Entities

### Pet

Main profile record for each pet.

Key fields:

- `id` (UUID, PK)
- `name` (required)
- `petType` (`CAT`, `DOG`, `BIRD`, `FISH`, `RABBIT`, `OTHER`)
- `birthDate` (nullable)
- `sex` (`UNKNOWN`, `MALE`, `FEMALE`)
- `breed`, `color`, `notes` (nullable)
- `microchipNumber`, `passportNumber` (nullable)
- `photoUri` (nullable)
- `createdAt`, `updatedAt`
- `deletedAt` (soft delete)
- `syncStatus`

### WeightEntry

Weight history per pet.

- `id` (PK)
- `petId` (FK)
- `date`
- `weightGrams`
- `note`
- audit + sync fields

### VaccinationEntry

Vaccination history per pet.

- `id` (PK)
- `petId` (FK)
- `vaccineType` (filtered by `petType` via `VaccineType.forPetType()`)
- `applicationDate`
- `nextDueDate`
- `veterinarian`, `clinic`, `batchNumber`, `note`
- audit + sync fields

### DewormingEntry

Deworming and antiparasitic history.

- `id` (PK)
- `petId` (FK)
- `type` (`INTERNAL`, `EXTERNAL`, `BOTH`)
- `medication` (nullable)
- `applicationDate`
- `nextDueDate`
- `note`
- audit + sync fields

### Task

Reminder/task scheduling model.

- `id` (PK)
- `petId` (nullable FK)
- `kind` (`WEIGHT`, `VACCINATION`, `DEWORMING`, `MEDICATION`, `CUSTOM`)
- `referenceEntityId` (nullable)
- `title`, `description`
- `scheduledFor`
- `repeatInterval`
- `status` (`ACTIVE`, `SNOOZED`, `COMPLETED`, `CANCELLED`)
- `lastTriggeredAt`
- `createdAt`, `updatedAt`

### SyncLog

Log of Nearby Connections family group sync operations.

- `id` (PK)
- `peerId`, `peerName`
- `syncTimestamp`
- `entitiesSent`, `entitiesReceived`, `conflictsResolved`
- `syncType`
- `createdAt`, `updatedAt`

## Relationships

- `Pet` 1:N `WeightEntry`
- `Pet` 1:N `VaccinationEntry`
- `Pet` 1:N `DewormingEntry`
- `Pet` 1:N `Task` (optional association)

## Vaccine Type Filtering

`VaccineType` enum entries are tagged with `applicablePetTypes`:

- Empty set = applicable to all species (e.g., `RABIES`)
- Non-empty set = species-specific (e.g., `FELV` and `FIV` for cats only; `DHPP`, `BORDETELLA`, `LEPTOSPIROSIS` for dogs)

Use `VaccineType.forPetType(petType)` to get the filtered list for a given species.

## Offline-First and Soft Delete

All core entities follow:

- soft delete (`deletedAt`)
- timestamp audit (`createdAt`, `updatedAt`)
- sync metadata (`syncStatus`)

Queries for active data must filter `deletedAt IS NULL`.

## Business Rules (Summary)

- Pet name is required.
- Birth date cannot be in the future.
- Weight is stored in grams.
- Date-driven health records support due-date reminders.
- Task scheduling supports repeat intervals.
- Vaccine types shown in UI are filtered by `petType`.

## Recommended Indexes

- Primary key index on all entities
- Foreign key index on all `petId` columns
- Optional index on task schedule/status for faster listing

## Evolution Notes

When adding new entities:

1. Keep Room as source of truth.
2. Include audit + soft delete + sync fields where applicable.
3. Add mapper and repository layers.
4. Add DAO tests for critical queries and soft-delete behavior.
5. If entity is pet-type-specific, add `petId` FK and ensure UI respects `petType`.
