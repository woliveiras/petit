package com.woliveiras.petit.domain.model

data class VaccinationTypeHistory(
  val vaccineType: VaccineType,
  val latest: VaccinationEntry,
  val history: List<VaccinationEntry>,
)

private val vaccinationRecencyComparator =
  compareByDescending<VaccinationEntry> { it.applicationDate }
    .thenByDescending { it.updatedAt }
    .thenByDescending { it.id }

fun List<VaccinationEntry>.groupVaccinationsByType(): List<VaccinationTypeHistory> =
  groupBy { it.vaccineType }
    .map { (type, entries) ->
      val history = entries.sortedWith(vaccinationRecencyComparator)
      VaccinationTypeHistory(vaccineType = type, latest = history.first(), history = history)
    }
    .sortedWith(
      compareByDescending<VaccinationTypeHistory> { it.latest.applicationDate }
        .thenByDescending { it.latest.updatedAt }
        .thenByDescending { it.latest.id }
    )
