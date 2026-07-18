package com.woliveiras.petit.presentation.feature.backup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupCompatibility
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.usecase.backup.BackupDeletionFailure
import com.woliveiras.petit.domain.usecase.backup.BackupDeletionFailureCategory
import com.woliveiras.petit.domain.usecase.backup.BackupDeletionResult
import com.woliveiras.petit.domain.usecase.backup.SavedBackupCollection
import java.time.Instant
import org.junit.Rule
import org.junit.Test

class SavedBackupsScreenComposeTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun emptyStateExposesCreateAction() {
    var createCalls = 0
    render(
      state = SavedBackupsUiState(content = SavedBackupsContent.Empty),
      onCreateBackup = { createCalls += 1 },
    )

    composeRule.onNodeWithText("No saved backups").assertIsDisplayed()
    composeRule.onNodeWithText("Create backup").assertIsDisplayed().performClick()

    assertThat(createCalls).isEqualTo(1)
  }

  @Test
  fun authorizationRequiredExposesReconnectAction() {
    var reconnectCalls = 0
    render(
      state =
        SavedBackupsUiState(
          authorization = BackupAuthorizationState.AuthorizationRequired,
          content = SavedBackupsContent.AuthorizationRequired,
        ),
      onReconnect = { reconnectCalls += 1 },
    )

    composeRule.onNodeWithText("Storage authorization required").assertIsDisplayed()
    composeRule.onNodeWithText("Reconnect").assertIsDisplayed().performClick()

    assertThat(reconnectCalls).isEqualTo(1)
  }

  @Test
  fun listExposesBackupAndOpensDetailsAction() {
    val backup = metadata("one")
    var shownRemoteId: String? = null
    render(
      state =
        SavedBackupsUiState(
          authorization = BackupAuthorizationState.Authorized(),
          content = SavedBackupsContent.Ready(collection(backup)),
        ),
      onShowDetails = { shownRemoteId = it },
    )

    composeRule.onNodeWithText("Backup one").assertIsDisplayed().performClick()
    assertThat(shownRemoteId).isEqualTo("remote-one")
  }

  @Test
  fun detailsExposeRestoreAndExactDeleteActions() {
    val backup = metadata("one")
    render(
      state =
        SavedBackupsUiState(
          authorization = BackupAuthorizationState.Authorized(),
          content = SavedBackupsContent.Ready(collection(backup)),
          details = backup,
        )
    )
    composeRule.onNodeWithText("Details one compatible").assertIsDisplayed()
    composeRule.onNodeWithText("Restore").assertIsDisplayed()
    composeRule.onNodeWithText("Delete").assertIsDisplayed()
  }

  @Test
  fun selectedBackupShowsCheckedControlAndDestructiveConfirmation() {
    val backup = metadata("one")
    var confirmCalls = 0
    render(
      state =
        SavedBackupsUiState(
          authorization = BackupAuthorizationState.Authorized(),
          content = SavedBackupsContent.Ready(collection(backup)),
          selectedRemoteIds = setOf(backup.remoteId),
          deletionConfirmation = BackupDeletionConfirmation.Selected(setOf(backup.remoteId)),
        ),
      onConfirmDeletion = { confirmCalls += 1 },
    )

    composeRule.onNodeWithText("Backup one").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Select backup one").assertIsOn()
    composeRule.onNodeWithText("Delete selected").assertIsDisplayed()
    composeRule.onNodeWithText("Delete selected backups permanently?").assertIsDisplayed()
    composeRule.onNodeWithText("Confirm permanent deletion").performClick()

    assertThat(confirmCalls).isEqualTo(1)
  }

  @Test
  fun partialFailureAnnouncesRetryAction() {
    val backup = metadata("two")
    val partial =
      BackupDeletionResult(
        deletedRemoteIds = setOf("remote-one"),
        failures =
          listOf(BackupDeletionFailure(backup.remoteId, BackupDeletionFailureCategory.RETRYABLE)),
      )
    var retryCalls = 0
    render(
      state =
        SavedBackupsUiState(
          authorization = BackupAuthorizationState.Authorized(),
          content = SavedBackupsContent.PartialDeletion(collection(backup), partial),
          selectedRemoteIds = setOf(backup.remoteId),
        ),
      onRetryFailedDeletion = { retryCalls += 1 },
    )

    composeRule.onNodeWithText("Some backups could not be deleted").assertIsDisplayed()
    composeRule.onNodeWithText("Retry failed deletions").assertIsDisplayed().performClick()

    assertThat(retryCalls).isEqualTo(1)
  }

  private fun render(
    state: SavedBackupsUiState,
    onReconnect: () -> Unit = {},
    onCreateBackup: () -> Unit = {},
    onShowDetails: (String) -> Unit = {},
    onConfirmDeletion: () -> Unit = {},
    onRetryFailedDeletion: () -> Unit = {},
  ) {
    composeRule.setContent {
      SavedBackupsScreen(
        state = state,
        copy = copy,
        onNavigateBack = {},
        onRefresh = {},
        onReconnect = onReconnect,
        onCreateBackup = onCreateBackup,
        onDisconnect = {},
        onToggleSelection = {},
        onShowDetails = onShowDetails,
        onDismissDetails = {},
        onRestore = {},
        onRequestDeleteOne = {},
        onRequestDeleteSelected = {},
        onRequestDeleteAll = {},
        onDismissDeletion = {},
        onConfirmDeletion = onConfirmDeletion,
        onRetryFailedDeletion = onRetryFailedDeletion,
      )
    }
  }

  private fun collection(vararg backups: BackupMetadata) =
    SavedBackupCollection(backups.toList(), backups.sumOf { it.archiveSizeBytes })

  private fun metadata(id: String) =
    BackupMetadata(
      remoteId = "remote-$id",
      backupId = id,
      createdAt = Instant.parse("2026-07-18T10:00:00Z"),
      trigger = BackupTrigger.MANUAL,
      appVersion = "1.0.0",
      archiveFormatVersion = 1,
      schemaVersion = 1,
      contentCounts = BackupContentCounts(pets = 1),
      archiveSizeBytes = 1,
      archiveSha256 = "sha256-$id",
      compatibility = BackupCompatibility.COMPATIBLE,
    )

  private companion object {
    val copy =
      SavedBackupsCopy(
        title = "Saved backups",
        loading = "Loading saved backups",
        authorizationRequired = "Storage authorization required",
        reconnect = "Reconnect",
        unavailable = "Storage unavailable",
        empty = "No saved backups",
        createBackup = "Create backup",
        retry = "Retry",
        deleteSelected = "Delete selected",
        deleteAll = "Delete all",
        disconnect = "Disconnect",
        partialDeletion = "Some backups could not be deleted",
        retryFailedDeletion = "Retry failed deletions",
        cancel = "Cancel",
        confirmDeletionTitle = "Permanent deletion",
        confirmSelectedDeletion = "Delete selected backups permanently?",
        confirmAllDeletion = "Delete every backup permanently?",
        confirmDelete = "Confirm permanent deletion",
        close = "Close",
        restore = "Restore",
        delete = "Delete",
        backupTitle = { "Backup ${it.backupId}" },
        backupSummary = { "${it.contentCounts.pets} pets, ${it.archiveSizeBytes} bytes" },
        selectBackup = { "Select backup ${it.backupId}" },
        collectionTotal = { count, bytes -> "$count backups, $bytes bytes" },
        backupDetails = { "Details ${it.backupId} ${it.compatibility.name.lowercase()}" },
        error = { "Backup error: ${it.name.lowercase()}" },
      )
  }
}
