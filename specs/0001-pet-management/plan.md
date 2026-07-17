# Plan: Pet management

Spec: [spec.md](./spec.md)

## Sequence

1. Model `PetEntity`, the domain, and mappers with creation, update, deletion, and synchronization metadata.
2. Expose CRUD operations and active queries through `PetDao` and `PetRepository`.
3. Implement validation and state in the ViewModels.
4. Integrate the list, details, form, deletion confirmation, and pet selection into navigation.
5. Integrate photo selection/capture and local storage.

## Architecture

- Room is the local source of truth; active queries filter by `deletedAt IS NULL`.
- ViewModels depend on `PetRepository`, never on the DAO.
- Routes: `pets`, `pets/{petId}`, `pets/form?petId={petId}`, `pets/{petId}/delete`, and `select-pet/{action}`.
- Deletion is soft and updates `deletedAt` and `updatedAt`.

## Dependencies and risks

- Foundation for all other specs in the `pet-care` family.
- Photo URIs require persistent permissions and handling for unavailable content.
