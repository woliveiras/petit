package com.woliveiras.petit.presentation.feature.backup

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.data.repository.BackupSettings
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import com.woliveiras.petit.domain.backup.BackupTrigger
import java.time.Instant
import org.junit.Rule
import org.junit.Test

class BackupSettingsComposeTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun settingsExposeAccessibleControlsHistoryInexactCopyAndDisconnectConfirmation() {
    val automatic = mutableListOf<Boolean>()
    val networks = mutableListOf<BackupNetworkRequirement>()
    val notifications = mutableListOf<Boolean>()
    var manualCalls = 0
    var disconnectCalls = 0
    val copy = copy()
    compose.setContent {
      MaterialTheme {
        BackupSettingsContent(
          state =
            BackupSettingsUiState(
              settings = BackupSettings(automaticBackupEnabled = true),
              authorization = BackupAuthorizationState.Authorized("Caregiver"),
              attempts = listOf(attempt()),
            ),
          copy = copy,
          onAuthorize = {},
          onAutomaticChanged = { automatic += it },
          onNetworkChanged = { networks += it },
          onNotifyChanged = { notifications += it },
          onBackUpNow = { manualCalls += 1 },
          onDisconnect = { disconnectCalls += 1 },
        )
      }
    }

    compose.onNodeWithText("Connected").assertIsDisplayed()
    compose.onNodeWithText(copy.inexactSchedule).assertIsDisplayed()
    compose.onNodeWithText("Automatic attempt succeeded").assertIsDisplayed()
    compose.onNodeWithContentDescription(copy.automaticBackup).performClick()
    compose.onNodeWithContentDescription(copy.unmeteredOnly).performClick()
    compose.onNodeWithContentDescription(copy.notifyAfterSuccess).performClick()
    compose.onNodeWithText(copy.backUpNow).performClick()
    compose.onNodeWithText(copy.disconnect).performClick()
    compose.onNodeWithText(copy.disconnectConfirmationMessage).assertIsDisplayed()
    compose.onNodeWithText(copy.confirmDisconnect).performClick()

    assertThat(automatic).containsExactly(false)
    assertThat(networks).containsExactly(BackupNetworkRequirement.CONNECTED)
    assertThat(notifications).containsExactly(true)
    assertThat(manualCalls).isEqualTo(1)
    assertThat(disconnectCalls).isEqualTo(1)
  }

  private fun attempt() =
    BackupAttempt(
      id = "attempt",
      trigger = BackupTrigger.AUTOMATIC,
      startedAt = Instant.EPOCH,
      completedAt = Instant.EPOCH,
      status = BackupAttemptStatus.SUCCEEDED,
    )

  private fun copy() =
    BackupSettingsCopy(
      title = "Backup settings",
      serviceState = "Storage connection",
      authorizationLabel = {
        if (it is BackupAuthorizationState.Authorized) "Connected" else "Disconnected"
      },
      connect = "Connect",
      automaticBackup = "Automatic backup",
      automaticBackupDescription = "Create backups automatically",
      inexactSchedule = "Android runs daily when conditions permit",
      unmeteredOnly = "Unmetered networks only",
      unmeteredDescription = "Avoid mobile data",
      notifyAfterSuccess = "Notify after backup",
      notifyDescription = "Show a silent completion notification",
      backUpNow = "Back up now",
      history = "History",
      noHistory = "No backup attempts",
      attemptSummary = { "Automatic attempt succeeded" },
      manualStatus = { it.name },
      disconnect = "Disconnect",
      disconnectConfirmationTitle = "Disconnect storage?",
      disconnectConfirmationMessage = "Remote backups will not be deleted",
      confirmDisconnect = "Disconnect storage",
      cancel = "Cancel",
      error = { it.name },
    )
}
