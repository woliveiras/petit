package com.woliveiras.petit.presentation.feature.pets

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.ui.theme.PetitTheme
import org.junit.Rule
import org.junit.Test

class PetDetailComposeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun shareProfileEntryPointStartsThePetExport() {
    var exportRequests = 0
    composeRule.setContent {
      PetitTheme {
        PetDetailContent(
          uiState =
            PetDetailUiState(
              isLoading = false,
              pet = Pet(id = "pet-1", name = "Mimi", createdAt = 1L, updatedAt = 1L),
            ),
          onWeightClick = {},
          onVaccinationsClick = {},
          onDewormingClick = {},
          onShareProfile = { exportRequests++ },
        )
      }
    }
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    composeRule
      .onNodeWithText(context.getString(R.string.pet_detail_section_share_profile))
      .performScrollTo()
      .performClick()

    assertThat(exportRequests).isEqualTo(1)
  }
}
