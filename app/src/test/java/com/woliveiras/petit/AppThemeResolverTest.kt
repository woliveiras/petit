package com.woliveiras.petit

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.AppTheme
import org.junit.Test

class AppThemeResolverTest {

  @Test
  fun systemThemeFollowsSystemAppearance() {
    assertThat(resolveDarkTheme(AppTheme.SYSTEM, systemIsDark = true)).isTrue()
    assertThat(resolveDarkTheme(AppTheme.SYSTEM, systemIsDark = false)).isFalse()
  }

  @Test
  fun explicitThemesOverrideSystemAppearance() {
    assertThat(resolveDarkTheme(AppTheme.LIGHT, systemIsDark = true)).isFalse()
    assertThat(resolveDarkTheme(AppTheme.DARK, systemIsDark = false)).isTrue()
  }
}
