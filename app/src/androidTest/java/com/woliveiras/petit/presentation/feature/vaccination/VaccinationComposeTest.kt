package com.woliveiras.petit.presentation.feature.vaccination

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.ui.theme.PetitTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class VaccinationComposeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun otherBranchShowsCustomNameAndValidationError() {
    val error = "Vaccine name is required"
    composeRule.setContent {
      PetitTheme {
        VaccinationCustomNameField(
          vaccineType = VaccineType.OTHER,
          customName = "",
          error = error,
          onValueChange = {},
        )
      }
    }

    composeRule.onNodeWithText(error).assertIsDisplayed()
  }

  @Test
  fun catalogTypeDoesNotShowCustomNameField() {
    composeRule.setContent {
      PetitTheme {
        VaccinationCustomNameField(
          vaccineType = VaccineType.V3,
          customName = "",
          error = null,
          onValueChange = {},
        )
      }
    }

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    composeRule
      .onAllNodesWithText(context.getString(R.string.vaccination_field_custom_name))
      .assertCountEquals(0)
  }

  @Test
  fun historyRendersOkScheduledAndOverdueStates() {
    val today = LocalDate.of(2026, 7, 17)
    val entries =
      listOf(
        entry("ok", VaccineType.RABIES, nextDueDate = null),
        entry("scheduled", VaccineType.V3, nextDueDate = today.plusDays(5)),
        entry("overdue", VaccineType.OTHER, nextDueDate = today.minusDays(1)),
      )
    composeRule.setContent {
      PetitTheme { VaccinationTimeline(vaccinations = entries, today = today, onEditEntry = {}) }
    }

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    listOf(
        context.getString(R.string.health_status_ok),
        context.getString(R.string.health_status_scheduled),
        context.getString(R.string.health_status_overdue),
      )
      .forEach { label -> composeRule.onAllNodesWithText(label)[0].assertIsDisplayed() }
  }

  @Test
  fun groupedHistoryKeepsOlderDoseAccessible() {
    val today = LocalDate.of(2026, 7, 17)
    val entries =
      listOf(
        entry("latest", VaccineType.RABIES, LocalDate.of(2026, 7, 1)),
        entry("older", VaccineType.RABIES, LocalDate.of(2025, 7, 1)),
      )
    composeRule.setContent {
      PetitTheme { VaccinationTimeline(vaccinations = entries, today = today, onEditEntry = {}) }
    }

    composeRule.onNode(hasText("01/07/2025")).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun historyDisplaysSavedTraceabilityDetails() {
    val today = LocalDate.of(2026, 7, 17)
    val detailed =
      entry("detailed", VaccineType.RABIES)
        .copy(
          veterinarian = "Dra. Ana",
          clinic = "Petit Vet",
          batchNumber = "lote-7",
          note = "reforço anual",
        )
    composeRule.setContent {
      PetitTheme {
        VaccinationTimeline(vaccinations = listOf(detailed), today = today, onEditEntry = {})
      }
    }

    listOf("Dra. Ana", "Petit Vet", "lote-7", "reforço anual").forEach { value ->
      composeRule.onNodeWithText(value).performScrollTo().assertIsDisplayed()
    }
  }

  @Test
  fun otherUsesCustomNameInAccessibleCardDescription() {
    val today = LocalDate.of(2026, 7, 17)
    val custom = entry("custom", VaccineType.OTHER)
    composeRule.setContent {
      PetitTheme {
        VaccinationTimeline(vaccinations = listOf(custom), today = today, onEditEntry = {})
      }
    }

    composeRule.onNode(hasContentDescription("Especial", substring = true)).assertIsDisplayed()
  }

  private fun entry(
    id: String,
    type: VaccineType,
    applicationDate: LocalDate = LocalDate.of(2026, 7, 1),
    nextDueDate: LocalDate? = null,
  ) =
    VaccinationEntry(
      id = id,
      petId = "pet-1",
      vaccineType = type,
      customVaccineTypeName = if (type == VaccineType.OTHER) "Especial" else null,
      applicationDate = applicationDate,
      nextDueDate = nextDueDate,
      createdAt = 1L,
      updatedAt = 1L,
    )
}
