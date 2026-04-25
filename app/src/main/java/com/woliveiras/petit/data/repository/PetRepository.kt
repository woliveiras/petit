package com.woliveiras.petit.data.repository

import com.woliveiras.petit.domain.model.Pet
import kotlinx.coroutines.flow.Flow

/** Repository interface for Pet operations. */
interface PetRepository {

  /** Get all active pets as a Flow. */
  fun getAllPets(): Flow<List<Pet>>

  /** Get a single pet by ID. */
  suspend fun getPetById(id: String): Pet?

  /** Get a single pet by ID as Flow. */
  fun getPetByIdFlow(id: String): Flow<Pet?>

  /** Get the count of active pets. */
  fun getPetCount(): Flow<Int>

  /** Insert or update a pet. */
  suspend fun savePet(pet: Pet)

  /** Soft delete a pet. */
  suspend fun deletePet(id: String)
}
