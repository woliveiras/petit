package com.woliveiras.petit.data.repository

import com.woliveiras.petit.domain.model.WeightEntry
import kotlinx.coroutines.flow.Flow

/** Repository interface for WeightEntry operations. */
interface WeightEntryRepository {

  /** Get all weight entries for a pet. */
  fun getWeightEntriesForPet(petId: String): Flow<List<WeightEntry>>

  /** Get the latest weight entry for a pet. */
  suspend fun getLatestWeightEntry(petId: String): WeightEntry?

  /** Get a weight entry by ID. */
  suspend fun getWeightEntryById(id: String): WeightEntry?

  /** Get the latest weight entry for a pet as Flow. */
  fun getLatestWeightEntryFlow(petId: String): Flow<WeightEntry?>

  /** Get weight entries for chart display. */
  fun getWeightEntriesForChart(petId: String, limit: Int = 30): Flow<List<WeightEntry>>

  /** Observe weight changes (lightweight trigger for UI refresh). */
  fun observeWeightChanges(): Flow<Long?>

  /**
   * Save a weight entry (insert or update). If there's already an entry for the same date, it will
   * be updated.
   */
  suspend fun saveWeightEntry(entry: WeightEntry)

  /** Delete a weight entry. */
  suspend fun deleteWeightEntry(id: String)

  /** Count weight entries for a pet. */
  suspend fun countEntriesForPet(petId: String): Int
}
