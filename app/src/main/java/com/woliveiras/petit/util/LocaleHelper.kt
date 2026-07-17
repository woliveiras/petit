package com.woliveiras.petit.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import com.woliveiras.petit.domain.model.AppLanguage
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class LanguageApplyResult {
  APPLIED,
  RESTART_REQUIRED,
}

interface LocaleApplicator {
  fun applyLanguage(context: Context, language: AppLanguage): LanguageApplyResult

  fun applyLanguageAtStartup(context: Context, language: AppLanguage)
}

/** Applies app-language choices according to the platform per-app locale capabilities. */
@Singleton
class LocaleHelper @Inject constructor() : LocaleApplicator {

  override fun applyLanguage(context: Context, language: AppLanguage): LanguageApplyResult {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      return LanguageApplyResult.RESTART_REQUIRED
    }

    context.getSystemService(LocaleManager::class.java)?.applicationLocales = language.localeList()
    return LanguageApplyResult.APPLIED
  }

  override fun applyLanguageAtStartup(context: Context, language: AppLanguage) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return

    val locale =
      when (language) {
        AppLanguage.SYSTEM -> Locale.getDefault()
        else -> Locale.forLanguageTag(language.code)
      }
    Locale.setDefault(locale)
    val configuration = context.resources.configuration
    configuration.setLocale(locale)
    context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
  }

  /** Returns the locale currently used by the app. */
  fun getCurrentLocale(context: Context): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
      if (locales != null && !locales.isEmpty) locales[0] ?: Locale.getDefault()
      else Locale.getDefault()
    } else {
      context.resources.configuration.locales[0] ?: Locale.getDefault()
    }
  }

  private fun AppLanguage.localeList(): LocaleList =
    when (this) {
      AppLanguage.SYSTEM -> LocaleList.getEmptyLocaleList()
      else -> LocaleList.forLanguageTags(code)
    }
}
