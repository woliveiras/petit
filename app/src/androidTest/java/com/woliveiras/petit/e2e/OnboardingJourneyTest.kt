package com.woliveiras.petit.e2e

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class OnboardingJourneyTest {
  private val composeRule = createAndroidComposeRule<MainActivity>()

  @get:Rule val rules: RuleChain = RuleChain.outerRule(ClearAppStateRule()).around(composeRule)

  @Test
  fun completeOnboarding_opensHomeAndPersistsCompletion() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val welcomeTitle = context.getString(R.string.onboarding_welcome_title)
    val featuresTitle = context.getString(R.string.onboarding_features_title)
    val ctaTitle = context.getString(R.string.onboarding_cta_title)
    val next = context.getString(R.string.onboarding_next)
    val getStarted = context.getString(R.string.onboarding_get_started)
    val registerPet = context.getString(R.string.home_register_pet)
    val homeNav = context.getString(R.string.nav_home)

    composeRule.onNodeWithText(welcomeTitle).assertIsDisplayed()
    composeRule.onAllNodesWithText(homeNav).assertCountEquals(0)

    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(featuresTitle).assertIsDisplayed()

    composeRule.onNodeWithText(next).performClick()
    composeRule.onNodeWithText(ctaTitle).assertIsDisplayed()

    composeRule.onNodeWithText(getStarted).performClick()
    composeRule.onNodeWithText(registerPet).assertIsDisplayed()
    composeRule.onNodeWithText(homeNav).assertIsDisplayed()

    composeRule.activityRule.scenario.recreate()

    composeRule.onNodeWithText(registerPet).assertIsDisplayed()
    composeRule.onAllNodesWithText(welcomeTitle).assertCountEquals(0)
  }
}
