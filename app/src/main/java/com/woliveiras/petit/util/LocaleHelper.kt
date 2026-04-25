package com.woliveiras.petit.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import com.woliveiras.petit.domain.model.AppLanguage
import java.util.Locale

/** Helper object for managing app locale changes. */
object LocaleHelper {

  /**
   * Apply the selected language to the app. On Android 13+, uses the system LocaleManager for
   * per-app language settings. On older versions, language change requires app restart.
   */
  fun applyLanguage(context: Context, language: AppLanguage) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val localeManager = context.getSystemService(LocaleManager::class.java)
      localeManager?.applicationLocales =
        when (language) {
          AppLanguage.SYSTEM -> LocaleList.getEmptyLocaleList()
          else -> LocaleList.forLanguageTags(language.code)
        }
    }
    // For older Android versions, the language preference is saved
    // but requires app restart to take effect
  }

  /** Get the current app locale. */
  fun getCurrentLocale(context: Context): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val localeManager = context.getSystemService(LocaleManager::class.java)
      val locales = localeManager?.applicationLocales
      if (locales != null && !locales.isEmpty) {
        locales.get(0) ?: Locale.getDefault()
      } else {
        Locale.getDefault()
      }
    } else {
      Locale.getDefault()
    }
  }
}
