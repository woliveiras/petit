package com.woliveiras.petit.data.repository

import com.woliveiras.petit.domain.model.VaccinationEntry
import kotlinx.coroutines.flow.Flow

/** Repository interface for VaccinationEntry operations. */
interface VaccinationEntryRepository {

  /** Get all vaccination entries for a pet. */
  fun getVaccinationEntriesForPet(petId: String): Flow<List<VaccinationEntry>>

  /** Get the latest vaccination for each type for a pet. */
  fun getLatestVaccinationsForPet(petId: String): Flow<List<VaccinationEntry>>

  /** Get a vaccination entry by ID. */
  suspend fun getVaccinationEntryById(id: String): VaccinationEntry?

  /** Get all overdue vaccinations. */
  fun getOverdueVaccinations(): Flow<List<VaccinationEntry>>

  /** Get upcoming vaccinations within a number of days. */
  fun getUpcomingVaccinations(days: Int = 30): Flow<List<VaccinationEntry>>

  /** Save a vaccination entry. */
  suspend fun saveVaccinationEntry(entry: VaccinationEntry)

  /** Delete a vaccination entry. */
  suspend fun deleteVaccinationEntry(id: String)

  /** Count vaccination entries for a pet. */
  suspend fun countEntriesForPet(petId: String): Int
}
