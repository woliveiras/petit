package com.woliveiras.petit.presentation.feature.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.ui.theme.PetitTheme
import org.junit.Rule
import org.junit.Test
import org.xmlpull.v1.XmlPullParser

class AppPreferencesComposeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun localeConfigAdvertisesOnlySelectableLocales() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val parser = context.resources.getXml(R.xml.locales_config)
    val locales = mutableListOf<String>()

    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
      if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
        locales += parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
      }
      parser.next()
    }

    assertThat(locales).containsExactly("en", "pt-BR").inOrder()
  }

  @Test
  fun themeSheetShowsOptionsMarksCurrentAndSelectsChoice() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    var selected: AppTheme? = null

    composeRule.setContent {
      PetitTheme {
        ThemeSelectionContent(
          currentTheme = AppTheme.DARK,
          themes = AppTheme.entries,
          onThemeSelected = { selected = it },
        )
      }
    }

    composeRule
      .onNodeWithText(context.getString(R.string.settings_theme_system))
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(context.getString(R.string.settings_theme_light))
      .assertIsNotSelected()
      .performClick()
    composeRule
      .onNodeWithText(context.getString(R.string.settings_theme_dark))
      .assertIsDisplayed()
      .assertIsSelected()
    composeRule
      .onAllNodesWithContentDescription(context.getString(R.string.cd_icon_check))
      .assertCountEquals(1)
    assertThat(selected).isEqualTo(AppTheme.LIGHT)
  }

  @Test
  fun languageSheetOffersOnlySupportedChoicesAndMarksCurrent() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    var selected: AppLanguage? = null

    composeRule.setContent {
      PetitTheme {
        LanguageSelectionContent(
          currentLanguage = AppLanguage.PORTUGUESE_BR,
          languages = AppLanguage.entries,
          onLanguageSelected = { selected = it },
        )
      }
    }

    composeRule
      .onNodeWithText(context.getString(R.string.settings_language_system))
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(context.getString(R.string.settings_language_english))
      .assertIsNotSelected()
      .performClick()
    composeRule
      .onNodeWithText(context.getString(R.string.settings_language_portuguese_br))
      .assertIsDisplayed()
      .assertIsSelected()
    composeRule
      .onAllNodesWithContentDescription(context.getString(R.string.cd_icon_check))
      .assertCountEquals(1)
    assertThat(selected).isEqualTo(AppLanguage.ENGLISH)
  }

  @Test
  fun savingStateDisablesLanguageChoices() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    composeRule.setContent {
      PetitTheme {
        LanguageSelectionContent(
          currentLanguage = AppLanguage.SYSTEM,
          languages = AppLanguage.entries,
          onLanguageSelected = {},
          enabled = false,
        )
      }
    }

    composeRule
      .onNodeWithText(context.getString(R.string.settings_language_english))
      .assertIsNotEnabled()
  }

  @Test
  fun preAndroid13RestartFeedbackAndFailureAreExplicit() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    composeRule.setContent {
      PetitTheme { LanguagePreferenceFeedback(restartRequired = true, error = "Write failed") }
    }

    composeRule
      .onNodeWithText(context.getString(R.string.settings_language_restart_message))
      .assertIsDisplayed()
    composeRule.onNodeWithText("Write failed").assertIsDisplayed()
  }
}
