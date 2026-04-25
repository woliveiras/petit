package com.woliveiras.petit.worker

import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.VaccinationEntry

/** Service responsible for creating automatic tasks based on health record changes. */
interface AutoTaskService {

  /** Create or update an automatic task when a vaccination entry is saved. */
  suspend fun handleVaccinationSaved(entry: VaccinationEntry)

  /** Handle vaccination entry deletion - remove associated task. */
  suspend fun handleVaccinationDeleted(entryId: String)

  /** Create or update an automatic task when a deworming entry is saved. */
  suspend fun handleDewormingSaved(entry: DewormingEntry)

  /** Handle deworming entry deletion - remove associated task. */
  suspend fun handleDewormingDeleted(entryId: String)

  /** Create or update a weight tracking task for a pet. */
  suspend fun handleWeightSaved(petId: String, petName: String)

  /** Cancel weight task for a pet. */
  suspend fun cancelWeightTask(petId: String)
}
