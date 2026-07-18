package com.woliveiras.petit.presentation.feature.backup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.ui.theme.PetitTheme
import org.junit.Rule
import org.junit.Test

class BackupSettingsCardComposeTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun unavailableProviderIsExplainedAndCannotReportACloudBackup() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    composeRule.setContent {
      PetitTheme {
        BackupSettingsCardContent(
          state =
            ManualBackupUiState(
              authorization = BackupAuthorizationState.Unavailable(),
              operation = ManualBackupOperation.Idle,
            ),
          onBackUp = {},
          onCancel = {},
        )
      }
    }

    composeRule.onNodeWithText(context.getString(R.string.backup_unavailable)).assertIsDisplayed()
    composeRule
      .onNodeWithText(context.getString(R.string.backup_now))
      .assertIsDisplayed()
      .assertIsNotEnabled()
  }
}
