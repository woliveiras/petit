package com.woliveiras.petit.util

import android.app.LocaleManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.AppLanguage
import java.util.Locale
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class LocaleHelperTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val localeHelper = LocaleHelper()
  private val originalLocale = Locale.getDefault()

  @After
  fun tearDown() {
    Locale.setDefault(originalLocale)
  }

  @Test
  @Config(sdk = [32])
  fun languageSelectionBeforeAndroid13RequiresRestartWithoutChangingLocale() {
    Locale.setDefault(Locale.ENGLISH)

    val result = localeHelper.applyLanguage(context, AppLanguage.PORTUGUESE_BR)

    assertThat(result).isEqualTo(LanguageApplyResult.RESTART_REQUIRED)
    assertThat(Locale.getDefault()).isEqualTo(Locale.ENGLISH)
  }

  @Test
  @Config(sdk = [32])
  fun startupBeforeAndroid13AppliesPersistedLanguage() {
    localeHelper.applyLanguageAtStartup(context, AppLanguage.PORTUGUESE_BR)

    assertThat(Locale.getDefault().toLanguageTag()).isEqualTo("pt-BR")
    assertThat(context.resources.configuration.locales[0].toLanguageTag()).isEqualTo("pt-BR")
  }

  @Test
  @Config(sdk = [33])
  fun android13SelectionUsesLocaleManagerWithoutRestartFeedback() {
    val result = localeHelper.applyLanguage(context, AppLanguage.ENGLISH)

    assertThat(result).isEqualTo(LanguageApplyResult.APPLIED)
    assertThat(localeHelper.getCurrentLocale(context).language).isEqualTo("en")
  }

  @Test
  @Config(sdk = [33])
  fun android13SystemSelectionClearsApplicationLocales() {
    localeHelper.applyLanguage(context, AppLanguage.PORTUGUESE_BR)

    val result = localeHelper.applyLanguage(context, AppLanguage.SYSTEM)

    assertThat(result).isEqualTo(LanguageApplyResult.APPLIED)
    assertThat(context.getSystemService(LocaleManager::class.java).applicationLocales.isEmpty)
      .isTrue()
  }
}
