package com.woliveiras.petit.data.repository

import com.woliveiras.petit.domain.model.DewormingEntry
import kotlinx.coroutines.flow.Flow

/** Repository interface for DewormingEntry operations. */
interface DewormingEntryRepository {

  /** Get all deworming entries for a pet. */
  fun getDewormingEntriesForPet(petId: String): Flow<List<DewormingEntry>>

  /** Get the latest deworming for each type for a pet. */
  fun getLatestDewormingsForPet(petId: String): Flow<List<DewormingEntry>>

  /** Get a deworming entry by ID. */
  suspend fun getDewormingEntryById(id: String): DewormingEntry?

  /** Get all overdue dewormings. */
  fun getOverdueDewormings(): Flow<List<DewormingEntry>>

  /** Get upcoming dewormings within a number of days. */
  fun getUpcomingDewormings(days: Int = 30): Flow<List<DewormingEntry>>

  /** Save a deworming entry. */
  suspend fun saveDewormingEntry(entry: DewormingEntry)

  /** Delete a deworming entry. */
  suspend fun deleteDewormingEntry(id: String)

  /** Count deworming entries for a pet. */
  suspend fun countEntriesForPet(petId: String): Int
}
