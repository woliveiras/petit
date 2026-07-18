package com.woliveiras.petit.presentation.feature.backup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.presentation.components.PetitTopAppBar

/**
 * Localized copy and formatting are injected by the host so this screen owns no shared resources.
 */
data class SavedBackupsCopy(
  val title: String,
  val loading: String,
  val authorizationRequired: String,
  val reconnect: String,
  val unavailable: String,
  val empty: String,
  val createBackup: String,
  val retry: String,
  val deleteSelected: String,
  val deleteAll: String,
  val disconnect: String,
  val partialDeletion: String,
  val retryFailedDeletion: String,
  val cancel: String,
  val confirmDeletionTitle: String,
  val confirmSelectedDeletion: String,
  val confirmAllDeletion: String,
  val confirmDelete: String,
  val close: String,
  val restore: String,
  val delete: String,
  val backupTitle: (BackupMetadata) -> String,
  val backupSummary: (BackupMetadata) -> String,
  val selectBackup: (BackupMetadata) -> String,
  val collectionTotal: (count: Int, bytes: Long) -> String,
  val backupDetails: (BackupMetadata) -> String,
  val error: (SavedBackupsErrorCategory) -> String,
)

@Composable
fun SavedBackupsScreen(
  state: SavedBackupsUiState,
  copy: SavedBackupsCopy,
  onNavigateBack: () -> Unit,
  onRefresh: () -> Unit,
  onReconnect: () -> Unit,
  onCreateBackup: () -> Unit,
  onDisconnect: () -> Unit,
  onToggleSelection: (String) -> Unit,
  onShowDetails: (String) -> Unit,
  onDismissDetails: () -> Unit,
  onRestore: (String) -> Unit,
  onRequestDeleteOne: (String) -> Unit,
  onRequestDeleteSelected: () -> Unit,
  onRequestDeleteAll: () -> Unit,
  onDismissDeletion: () -> Unit,
  onConfirmDeletion: () -> Unit,
  onRetryFailedDeletion: () -> Unit,
) {
  Scaffold(
    topBar = { PetitTopAppBar(title = { Text(copy.title) }, onNavigateBack = onNavigateBack) }
  ) { padding ->
    SavedBackupsRouteContent(
      state = state,
      copy = copy,
      modifier = Modifier.fillMaxSize().padding(padding),
      onRefresh = onRefresh,
      onReconnect = onReconnect,
      onCreateBackup = onCreateBackup,
      onDisconnect = onDisconnect,
      onToggleSelection = onToggleSelection,
      onShowDetails = onShowDetails,
      onRequestDeleteSelected = onRequestDeleteSelected,
      onRequestDeleteAll = onRequestDeleteAll,
      onRetryFailedDeletion = onRetryFailedDeletion,
    )
  }

  state.details?.let { details ->
    AlertDialog(
      onDismissRequest = onDismissDetails,
      title = { Text(copy.backupTitle(details), modifier = Modifier.semantics { heading() }) },
      text = { Text(copy.backupDetails(details)) },
      confirmButton = { Button(onClick = { onRestore(details.remoteId) }) { Text(copy.restore) } },
      dismissButton = {
        Row {
          TextButton(onClick = { onRequestDeleteOne(details.remoteId) }) { Text(copy.delete) }
          TextButton(onClick = onDismissDetails) { Text(copy.close) }
        }
      },
    )
  }

  state.deletionConfirmation?.let { confirmation ->
    val message =
      when (confirmation) {
        is BackupDeletionConfirmation.All -> copy.confirmAllDeletion
        is BackupDeletionConfirmation.Selected -> copy.confirmSelectedDeletion
      }
    AlertDialog(
      onDismissRequest = onDismissDeletion,
      title = { Text(copy.confirmDeletionTitle, modifier = Modifier.semantics { heading() }) },
      text = { Text(message) },
      confirmButton = { Button(onClick = onConfirmDeletion) { Text(copy.confirmDelete) } },
      dismissButton = { TextButton(onClick = onDismissDeletion) { Text(copy.cancel) } },
    )
  }
}

@Composable
fun SavedBackupsRouteContent(
  state: SavedBackupsUiState,
  copy: SavedBackupsCopy,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit,
  onReconnect: () -> Unit,
  onCreateBackup: () -> Unit,
  onDisconnect: () -> Unit,
  onToggleSelection: (String) -> Unit,
  onShowDetails: (String) -> Unit,
  onRequestDeleteSelected: () -> Unit,
  onRequestDeleteAll: () -> Unit,
  onRetryFailedDeletion: () -> Unit,
) {
  when (val content = state.content) {
    SavedBackupsContent.Loading ->
      CenteredContent(modifier) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = copy.loading })
      }
    SavedBackupsContent.AuthorizationRequired ->
      CenteredAction(modifier, copy.authorizationRequired, copy.reconnect, onReconnect)
    is SavedBackupsContent.Unavailable -> CenteredContent(modifier) { Text(copy.unavailable) }
    SavedBackupsContent.Empty ->
      CenteredAction(modifier, copy.empty, copy.createBackup, onCreateBackup)
    is SavedBackupsContent.Error ->
      CenteredContent(modifier) {
        Text(
          copy.error(content.category),
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        )
        Button(onClick = onRefresh) { Text(copy.retry) }
      }
    is SavedBackupsContent.Ready ->
      BackupList(
        modifier,
        content.collection,
        state.selectedRemoteIds,
        copy,
        onDisconnect,
        onToggleSelection,
        onShowDetails,
        onRequestDeleteSelected,
        onRequestDeleteAll,
      )
    is SavedBackupsContent.PartialDeletion ->
      Column(modifier) {
        Row(
          Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            copy.partialDeletion,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
          )
          Button(onClick = onRetryFailedDeletion) { Text(copy.retryFailedDeletion) }
        }
        BackupList(
          Modifier.weight(1f),
          content.collection,
          state.selectedRemoteIds,
          copy,
          onDisconnect,
          onToggleSelection,
          onShowDetails,
          onRequestDeleteSelected,
          onRequestDeleteAll,
        )
      }
  }
}

@Composable
private fun BackupList(
  modifier: Modifier,
  collection: com.woliveiras.petit.domain.usecase.backup.SavedBackupCollection,
  selectedRemoteIds: Set<String>,
  copy: SavedBackupsCopy,
  onDisconnect: () -> Unit,
  onToggleSelection: (String) -> Unit,
  onShowDetails: (String) -> Unit,
  onRequestDeleteSelected: () -> Unit,
  onRequestDeleteAll: () -> Unit,
) {
  Column(modifier) {
    Text(
      copy.collectionTotal(collection.backups.size, collection.totalArchiveSizeBytes),
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (selectedRemoteIds.isNotEmpty()) {
        Button(onClick = onRequestDeleteSelected) { Text(copy.deleteSelected) }
      }
      OutlinedButton(onClick = onRequestDeleteAll) { Text(copy.deleteAll) }
      OutlinedButton(onClick = onDisconnect) { Text(copy.disconnect) }
    }
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(collection.backups, key = BackupMetadata::remoteId) { backup ->
        Card(Modifier.fillMaxWidth().clickable { onShowDetails(backup.remoteId) }) {
          Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Checkbox(
              checked = backup.remoteId in selectedRemoteIds,
              onCheckedChange = { onToggleSelection(backup.remoteId) },
              modifier = Modifier.semantics { contentDescription = copy.selectBackup(backup) },
            )
            Column(Modifier.weight(1f)) {
              Text(
                copy.backupTitle(backup),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
              )
              Text(copy.backupSummary(backup))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CenteredAction(
  modifier: Modifier,
  message: String,
  action: String,
  onClick: () -> Unit,
) {
  CenteredContent(modifier) {
    Text(message)
    Button(onClick = onClick) { Text(action) }
  }
}

@Composable
private fun CenteredContent(modifier: Modifier, content: @Composable () -> Unit) {
  Box(modifier, contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      content()
    }
  }
}
