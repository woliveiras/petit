package com.woliveiras.petit.domain.model

import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Domain model representing a vaccination record. */
data class VaccinationEntry(
  val id: String,
  val petId: String,
  val vaccineType: VaccineType,
  val customVaccineTypeName: String? = null,
  val applicationDate: LocalDate,
  val nextDueDate: LocalDate? = null,
  val veterinarian: String? = null,
  val clinic: String? = null,
  val batchNumber: String? = null,
  val note: String? = null,
  val createdAt: Long,
  val updatedAt: Long,
  val deletedAt: Long? = null,
  val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
) {

  /** Returns the display name for this vaccine (uses customVaccineTypeName when type is OTHER). */
  val effectiveVaccineDisplayName: String
    get() =
      if (vaccineType == VaccineType.OTHER && !customVaccineTypeName.isNullOrBlank()) {
        customVaccineTypeName
      } else {
        vaccineType.displayName
      }

  /** Whether this vaccination is overdue. */
  val isOverdue: Boolean
    get() = isOverdue(Clock.systemDefaultZone())

  fun isOverdue(clock: Clock): Boolean = isOverdue(LocalDate.now(clock))

  fun isOverdue(onDate: LocalDate): Boolean = nextDueDate?.isBefore(onDate) == true

  /**
   * Calculates the current status based on nextDueDate.
   * - OVERDUE: nextDueDate is in the past
   * - SCHEDULED: nextDueDate is within the next 30 days
   * - OK: nextDueDate is more than 30 days away or null
   */
  val status: HealthStatus
    get() = status(Clock.systemDefaultZone())

  fun status(clock: Clock): HealthStatus = status(LocalDate.now(clock))

  fun status(onDate: LocalDate): HealthStatus {
    val dueDate = nextDueDate ?: return HealthStatus.OK
    val daysUntilDue = ChronoUnit.DAYS.between(onDate, dueDate)
    return when {
      daysUntilDue < 0 -> HealthStatus.OVERDUE
      daysUntilDue <= 30 -> HealthStatus.SCHEDULED
      else -> HealthStatus.OK
    }
  }

  companion object
}
