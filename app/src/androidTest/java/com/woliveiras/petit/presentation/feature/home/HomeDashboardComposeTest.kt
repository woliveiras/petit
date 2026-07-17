package com.woliveiras.petit.presentation.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.HealthStatus
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.ui.theme.PetitTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class HomeDashboardComposeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun healthyPetsShowAccessibleAllGoodBanner() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    composeRule.setContent {
      PetitTheme {
        HomeContent(
          uiState =
            homeUiState(pets = listOf(petWithSummary("Mimi", HealthStatus.OK)), isAllGood = true),
          onPetClick = {},
          onTaskClick = {},
          onCompleteTask = {},
          onSeeAllPets = {},
          onSeeAllTasks = {},
          onTimelineClick = {},
        )
      }
    }

    composeRule.onNodeWithText(context.getString(R.string.home_all_good)).assertIsDisplayed()
    composeRule
      .onNode(hasContentDescription(context.getString(R.string.home_all_good), substring = true))
      .assertIsDisplayed()
  }

  @Test
  fun alertsAppearBeforePetCardsAndExposeStatusAndDate() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val overdue = petWithSummary("Mimi", HealthStatus.OVERDUE, LocalDate.of(2026, 7, 1))
    val scheduled = petWithSummary("Luna", HealthStatus.SCHEDULED, LocalDate.of(2026, 7, 20))
    composeRule.setContent {
      PetitTheme {
        HomeContent(
          uiState =
            homeUiState(
              pets = listOf(overdue, scheduled),
              alerts =
                listOf(
                  HomeAlert(overdue, LocalDate.of(2026, 7, 1)),
                  HomeAlert(scheduled, LocalDate.of(2026, 7, 20)),
                ),
            ),
          onPetClick = {},
          onTaskClick = {},
          onCompleteTask = {},
          onSeeAllPets = {},
          onSeeAllTasks = {},
          onTimelineClick = {},
        )
      }
    }

    composeRule.onNodeWithText(context.getString(R.string.home_section_alerts)).assertIsDisplayed()
    composeRule
      .onNodeWithText(context.getString(R.string.home_alert_relevant_date, "01/07/2026"))
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(context.getString(R.string.home_alert_relevant_date, "20/07/2026"))
      .assertIsDisplayed()
  }

  @Test
  fun compactPetCardExposesOverallHealthStatus() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    composeRule.setContent {
      PetitTheme {
        HomeContent(
          uiState = homeUiState(pets = listOf(petWithSummary("Mimi", HealthStatus.OVERDUE))),
          onPetClick = {},
          onTaskClick = {},
          onCompleteTask = {},
          onSeeAllPets = {},
          onSeeAllTasks = {},
          onTimelineClick = {},
        )
      }
    }

    composeRule
      .onNodeWithContentDescription(
        "Mimi, ${context.getString(R.string.health_status_overdue)}",
        substring = true,
      )
      .assertIsDisplayed()
  }

  @Test
  fun horizontalPetCardExposesOverallHealthStatus() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val pets =
      listOf(
        petWithSummary("Mimi", HealthStatus.OVERDUE),
        petWithSummary("Luna", HealthStatus.OK),
        petWithSummary("Nina", HealthStatus.OK),
        petWithSummary("Theo", HealthStatus.OK),
      )
    composeRule.setContent {
      PetitTheme {
        HomeContent(
          uiState = homeUiState(pets = pets),
          onPetClick = {},
          onTaskClick = {},
          onCompleteTask = {},
          onSeeAllPets = {},
          onSeeAllTasks = {},
          onTimelineClick = {},
        )
      }
    }

    composeRule
      .onNodeWithContentDescription(
        "Mimi, ${context.getString(R.string.health_status_overdue)}",
        substring = true,
      )
      .assertIsDisplayed()
  }

  private fun homeUiState(
    pets: List<PetWithSummary>,
    isAllGood: Boolean = false,
    alerts: List<HomeAlert> = emptyList(),
  ) = HomeUiState(isLoading = false, pets = pets, isAllGood = isAllGood, alerts = alerts)

  private fun petWithSummary(
    name: String,
    status: HealthStatus,
    nextVaccinationDate: LocalDate? = null,
  ) =
    PetWithSummary(
      pet = Pet(id = name, name = name, petType = PetType.CAT, createdAt = 1L, updatedAt = 1L),
      overallStatus = status,
      nextVaccinationDate = nextVaccinationDate,
    )
}
