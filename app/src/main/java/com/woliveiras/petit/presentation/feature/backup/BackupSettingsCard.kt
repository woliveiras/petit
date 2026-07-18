package com.woliveiras.petit.presentation.feature.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.backup.BackupAuthorizationState

@Composable
fun BackupSettingsCard(viewModel: ManualBackupViewModel = hiltViewModel()) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  BackupSettingsCardContent(
    state = state,
    onBackUp = {
      if (state.authorization is BackupAuthorizationState.Authorized) {
        viewModel.backUpNow()
      } else {
        viewModel.authorizeAndBackUp()
      }
    },
    onCancel = viewModel::cancel,
  )
}

@Composable
internal fun BackupSettingsCardContent(
  state: ManualBackupUiState,
  onBackUp: () -> Unit,
  onCancel: () -> Unit,
) {
  val operation = state.operation
  val isBusy =
    operation is ManualBackupOperation.Authorizing || operation is ManualBackupOperation.Creating
  val unavailable = state.authorization is BackupAuthorizationState.Unavailable
  val status = backupStatusText(state)

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      ListItem(
        colors =
          ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        leadingContent = {
          Icon(
            imageVector = Icons.Default.CloudUpload,
            contentDescription = stringResource(R.string.backup_settings_title),
            tint = MaterialTheme.colorScheme.primary,
          )
        },
        headlineContent = { Text(stringResource(R.string.backup_settings_title)) },
        supportingContent = { Text(status) },
      )

      if (operation is ManualBackupOperation.Creating) {
        val progress = operation.progress
        if (progress == null || progress.totalBytes == 0L) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        } else {
          LinearProgressIndicator(
            progress = { progress.bytesTransferred.toFloat() / progress.totalBytes.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          )
        }
      }

      Button(
        onClick = if (isBusy) onCancel else onBackUp,
        enabled = !unavailable,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
      ) {
        Text(stringResource(if (isBusy) R.string.backup_cancel else R.string.backup_now))
      }
    }
  }
}

@Composable
private fun backupStatusText(state: ManualBackupUiState): String =
  when (val operation = state.operation) {
    ManualBackupOperation.Idle ->
      when (state.authorization) {
        BackupAuthorizationState.Disconnected,
        BackupAuthorizationState.AuthorizationRequired ->
          stringResource(R.string.backup_authorization_required)
        BackupAuthorizationState.Authorizing -> stringResource(R.string.backup_authorizing)
        is BackupAuthorizationState.Authorized -> stringResource(R.string.backup_ready)
        is BackupAuthorizationState.Unavailable -> stringResource(R.string.backup_unavailable)
      }
    ManualBackupOperation.Authorizing -> stringResource(R.string.backup_authorizing)
    ManualBackupOperation.AuthorizationRequired ->
      stringResource(R.string.backup_authorization_required)
    is ManualBackupOperation.Creating -> stringResource(R.string.backup_in_progress)
    is ManualBackupOperation.Complete ->
      stringResource(R.string.backup_complete, operation.metadata.archiveSizeBytes)
    ManualBackupOperation.QuotaExceeded -> stringResource(R.string.backup_quota_exceeded)
    is ManualBackupOperation.RetryableFailure -> stringResource(R.string.backup_retryable_error)
    is ManualBackupOperation.PermanentFailure -> stringResource(R.string.backup_permanent_error)
    is ManualBackupOperation.Unavailable -> stringResource(R.string.backup_unavailable)
  }
