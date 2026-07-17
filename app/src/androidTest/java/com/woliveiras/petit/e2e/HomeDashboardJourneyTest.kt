package com.woliveiras.petit.e2e

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.woliveiras.petit.MainActivity
import com.woliveiras.petit.R
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class HomeDashboardJourneyTest {
  private val composeRule = createAndroidComposeRule<MainActivity>()

  @get:Rule val rules: RuleChain = RuleChain.outerRule(ClearAppStateRule()).around(composeRule)

  @Test
  fun savedVaccinationReachesDashboardHealthIndicator() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val next = context.getString(R.string.onboarding_next)
    val getStarted = context.getString(R.string.onboarding_get_started)
    val registerPet = context.getString(R.string.home_register_pet)
    val namePlaceholder = context.getString(R.string.pet_form_name_placeholder)
    val save = context.getString(R.string.action_save)
    val vaccinations = context.getString(R.string.pet_detail_section_vaccinations)
    val addVaccination = context.getString(R.string.vaccination_add)
    val rabies = context.getString(R.string.vaccine_rabies)
    val petName = "Mimi"

    composeRule.onAllNodesWithText(next)[0].performClick()
    composeRule.onAllNodesWithText(next)[0].performClick()
    composeRule.onAllNodesWithText(getStarted)[0].performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodesWithText(registerPet, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    composeRule.onNodeWithText(registerPet, useUnmergedTree = true).performClick()
    composeRule.onNodeWithText(namePlaceholder).performTextInput(petName)
    closeSoftKeyboard()
    composeRule.onNodeWithText(save).performScrollTo().performClick()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodesWithContentDescription(context.getString(R.string.action_edit))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    composeRule.onNodeWithText(vaccinations).performScrollTo().performClick()
    composeRule.onNodeWithContentDescription(addVaccination).performClick()
    composeRule.onNodeWithText(rabies).performClick()
    composeRule.onNodeWithText(save).performScrollTo().performClick()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodes(hasContentDescription(rabies, substring = true))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    pressBack()
    pressBack()

    val upToDate = context.getString(R.string.health_status_ok)
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodes(hasContentDescription("$petName, $upToDate", substring = true))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
  }
}
