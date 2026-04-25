package com.woliveiras.petit.domain.model

import java.time.LocalDate

/** Aggregated health summary for a single pet. */
data class PetHealthSummary(
  val petId: String,
  val latestWeight: WeightEntry? = null,
  val weightStatus: WeightStatus = WeightStatus.NO_DATA,
  val overallStatus: HealthStatus = HealthStatus.OK,
  val nextVaccineType: VaccineType? = null,
  val nextVaccinationDate: LocalDate? = null,
  val nextDewormingType: DewormingType? = null,
  val nextDewormingDate: LocalDate? = null,
)

/** Weight tracking status for display. */
enum class WeightStatus {
  /** No weight data registered. */
  NO_DATA,

  /** Weight updated within the last 15 days. */
  UPDATED,

  /** Weight data is older than 15 days. */
  OUTDATED,
}
