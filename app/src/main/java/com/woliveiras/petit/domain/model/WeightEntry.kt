package com.woliveiras.petit.domain.model

import java.time.LocalDate

/** Domain model representing a weight measurement entry. */
data class WeightEntry(
  val id: String,
  val petId: String,
  val date: LocalDate,
  val weightGrams: Int,
  val note: String? = null,
  val createdAt: Long,
  val updatedAt: Long,
  val deletedAt: Long? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
) {
  /** Weight in kilograms. */
  val weightKg: Double
    get() = weightGrams / 1000.0

  /** Formatted weight string (e.g., "3.5 kg"). */
  val formattedWeight: String
    get() = String.format("%.1f kg", weightKg)

  companion object
}
