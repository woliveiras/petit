package com.woliveiras.petit.presentation.feature.home

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.HealthStatus
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.Sex
import java.time.LocalDate
import org.junit.Test

class HomeDashboardStateTest {

  @Test
  fun allPetsHealthyShowsAllGoodAndNoAlerts() {
    val dashboard =
      HomeDashboardState.from(
        listOf(
          petWithSummary(id = "mimi", status = HealthStatus.OK),
          petWithSummary(id = "luna", status = HealthStatus.OK),
        )
      )

    assertThat(dashboard.isAllGood).isTrue()
    assertThat(dashboard.alerts).isEmpty()
  }

  @Test
  fun unavailableHealthSummaryDoesNotShowAllGood() {
    val dashboard =
      HomeDashboardState.from(
        listOf(
          petWithSummary(id = "mimi", status = HealthStatus.OK)
            .copy(isHealthSummaryAvailable = false, overallStatus = HealthStatus.OVERDUE)
        )
      )

    assertThat(dashboard.isAllGood).isFalse()
    assertThat(dashboard.alerts).isEmpty()
  }

  @Test
  fun alertsPrioritizeOverdueBeforeScheduledThenRelevantDate() {
    val dashboard =
      HomeDashboardState.from(
        listOf(
          petWithSummary(
            id = "scheduled-later",
            status = HealthStatus.SCHEDULED,
            vaccinationDate = LocalDate.of(2026, 8, 10),
          ),
          petWithSummary(
            id = "overdue-later",
            status = HealthStatus.OVERDUE,
            dewormingDate = LocalDate.of(2026, 7, 5),
          ),
          petWithSummary(
            id = "overdue-earlier",
            status = HealthStatus.OVERDUE,
            vaccinationDate = LocalDate.of(2026, 7, 1),
          ),
          petWithSummary(
            id = "scheduled-earlier",
            status = HealthStatus.SCHEDULED,
            vaccinationDate = LocalDate.of(2026, 7, 20),
          ),
        )
      )

    assertThat(dashboard.isAllGood).isFalse()
    assertThat(dashboard.alerts.map { it.petWithSummary.pet.id })
      .containsExactly("overdue-earlier", "overdue-later", "scheduled-earlier", "scheduled-later")
      .inOrder()
  }

  @Test
  fun alertUsesEarliestCareDateAsRelevantDate() {
    val dashboard =
      HomeDashboardState.from(
        listOf(
          petWithSummary(
            id = "mimi",
            status = HealthStatus.SCHEDULED,
            vaccinationDate = LocalDate.of(2026, 8, 1),
            dewormingDate = LocalDate.of(2026, 7, 24),
          )
        )
      )

    assertThat(dashboard.alerts.single().relevantDate).isEqualTo(LocalDate.of(2026, 7, 24))
  }

  private fun petWithSummary(
    id: String,
    status: HealthStatus,
    vaccinationDate: LocalDate? = null,
    dewormingDate: LocalDate? = null,
  ) =
    PetWithSummary(
      pet =
        Pet(
          id = id,
          name = id,
          petType = PetType.CAT,
          sex = Sex.UNKNOWN,
          createdAt = 1L,
          updatedAt = 1L,
        ),
      overallStatus = status,
      nextVaccinationDate = vaccinationDate,
      nextDewormingDate = dewormingDate,
    )
}
