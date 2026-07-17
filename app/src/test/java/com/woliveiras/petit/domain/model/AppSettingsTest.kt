package com.woliveiras.petit.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppSettingsTest {

  @Test
  fun languageCodesExposeOnlyIntentionalSelectionsAndFallbackToSystem() {
    assertThat(AppLanguage.entries.map { it.code })
      .containsExactly("system", "en", "pt-BR")
      .inOrder()
    assertThat(AppLanguage.fromCode("es")).isEqualTo(AppLanguage.SYSTEM)
    assertThat(AppLanguage.fromCode("unknown")).isEqualTo(AppLanguage.SYSTEM)
  }
}
