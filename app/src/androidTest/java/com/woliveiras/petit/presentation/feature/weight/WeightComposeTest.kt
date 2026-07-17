package com.woliveiras.petit.presentation.feature.weight

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.WeightEntry
import com.woliveiras.petit.ui.theme.PetitTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class WeightComposeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun formExposesDecimalInputUnitSelectionAndValidationError() {
    var entered = ""
    var selectedUnit = WeightUnit.KG
    val error = "Invalid weight"
    composeRule.setContent {
      PetitTheme {
        WeightInputCard(
          weightValue = entered,
          weightUnit = selectedUnit,
          onWeightChange = { entered = it },
          onUnitChange = { selectedUnit = it },
          weightError = error,
        )
      }
    }

    composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("3,5")
    composeRule.onNodeWithText("g").performClick()

    assertThat(entered).isEqualTo("3,5")
    assertThat(selectedUnit).isEqualTo(WeightUnit.GRAMS)
    composeRule.onNodeWithText(error).assertIsDisplayed()
  }

  @Test
  fun historySortsDescendingAndChartDescribesChronologicalKilogramValues() {
    var editedId: String? = null
    val oldest = entry("oldest", day = 1, grams = 4_000)
    val latest = entry("latest", day = 20, grams = 4_500)
    composeRule.setContent {
      PetitTheme {
        WeightHistoryContent(
          entries = listOf(latest, oldest),
          onNavigateToEditEntry = { editedId = it },
        )
      }
    }
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val chartDescription =
      context.getString(R.string.weight_chart_description, "4.0", "01/07", "4.5", "20/07")

    composeRule.onNode(hasContentDescription(chartDescription)).assertIsDisplayed()
    val latestNode =
      composeRule
        .onNode(hasContentDescription("4.5 kg, 20/07/2026", substring = true))
        .fetchSemanticsNode()
    val oldestNode =
      composeRule
        .onNode(hasContentDescription("4.0 kg, 01/07/2026", substring = true))
        .fetchSemanticsNode()
    assertThat(latestNode.boundsInRoot.top).isLessThan(oldestNode.boundsInRoot.top)
    composeRule.onNode(hasContentDescription("4.5 kg, 20/07/2026", substring = true)).performClick()
    assertThat(editedId).isEqualTo("latest")
  }

  private fun entry(id: String, day: Int, grams: Int) =
    WeightEntry(
      id = id,
      petId = "pet-1",
      date = LocalDate.of(2026, 7, day),
      weightGrams = grams,
      createdAt = day.toLong(),
      updatedAt = day.toLong(),
    )
}
