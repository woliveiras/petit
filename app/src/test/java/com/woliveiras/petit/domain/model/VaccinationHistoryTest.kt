package com.woliveiras.petit.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class VaccinationHistoryTest {

  @Test
  fun groupsByTypeSelectsLatestDeterministicallyAndPreservesEveryDose() {
    val oldRabies = entry("rabies-old", VaccineType.RABIES, LocalDate.of(2025, 7, 1), 100L)
    val sameDayOlderUpdate = entry("rabies-a", VaccineType.RABIES, LocalDate.of(2026, 7, 1), 200L)
    val latestRabies = entry("rabies-b", VaccineType.RABIES, LocalDate.of(2026, 7, 1), 300L)
    val v3 = entry("v3", VaccineType.V3, LocalDate.of(2026, 6, 1), 400L)

    val groups = listOf(oldRabies, sameDayOlderUpdate, v3, latestRabies).groupVaccinationsByType()

    assertThat(groups.map { it.vaccineType }).containsExactly(VaccineType.RABIES, VaccineType.V3)
    assertThat(groups.first().latest.id).isEqualTo("rabies-b")
    assertThat(groups.first().history.map { it.id })
      .containsExactly("rabies-b", "rabies-a", "rabies-old")
      .inOrder()
  }

  @Test
  fun idBreaksATieWhenDatesAndUpdateTimestampsMatch() {
    val first = entry("entry-a", VaccineType.V3, LocalDate.of(2026, 7, 1), 300L)
    val second = entry("entry-b", VaccineType.V3, LocalDate.of(2026, 7, 1), 300L)

    assertThat(listOf(first, second).groupVaccinationsByType().single().latest.id)
      .isEqualTo("entry-b")
  }

  private fun entry(id: String, type: VaccineType, date: LocalDate, updatedAt: Long) =
    VaccinationEntry(
      id = id,
      petId = "pet-1",
      vaccineType = type,
      applicationDate = date,
      createdAt = 1L,
      updatedAt = updatedAt,
    )
}
