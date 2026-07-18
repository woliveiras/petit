package com.woliveiras.petit.presentation.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import com.woliveiras.petit.presentation.components.PetitTopAppBar

/** Localized copy is injected by the host so this screen does not own shared resources. */
data class BackupSettingsCopy(
  val title: String,
  val serviceState: String,
  val authorizationLabel: (BackupAuthorizationState) -> String,
  val connect: String,
  val automaticBackup: String,
  val automaticBackupDescription: String,
  val inexactSchedule: String,
  val unmeteredOnly: String,
  val unmeteredDescription: String,
  val notifyAfterSuccess: String,
  val notifyDescription: String,
  val backUpNow: String,
  val history: String,
  val noHistory: String,
  val attemptSummary: (BackupAttempt) -> String,
  val manualStatus: (BackupAttemptStatus) -> String,
  val disconnect: String,
  val disconnectConfirmationTitle: String,
  val disconnectConfirmationMessage: String,
  val confirmDisconnect: String,
  val cancel: String,
  val error: (BackupSettingsError) -> String,
)

@Composable
fun BackupSettingsScreen(
  state: BackupSettingsUiState,
  copy: BackupSettingsCopy,
  onNavigateBack: () -> Unit,
  onAuthorize: () -> Unit,
  onAutomaticChanged: (Boolean) -> Unit,
  onNetworkChanged: (BackupNetworkRequirement) -> Unit,
  onNotifyChanged: (Boolean) -> Unit,
  onBackUpNow: () -> Unit,
  onDisconnect: () -> Unit,
) {
  Scaffold(
    topBar = { PetitTopAppBar(title = { Text(copy.title) }, onNavigateBack = onNavigateBack) }
  ) { padding ->
    BackupSettingsContent(
      state = state,
      copy = copy,
      modifier = Modifier.fillMaxSize().padding(padding),
      onAuthorize = onAuthorize,
      onAutomaticChanged = onAutomaticChanged,
      onNetworkChanged = onNetworkChanged,
      onNotifyChanged = onNotifyChanged,
      onBackUpNow = onBackUpNow,
      onDisconnect = onDisconnect,
    )
  }
}

@Composable
fun BackupSettingsContent(
  state: BackupSettingsUiState,
  copy: BackupSettingsCopy,
  modifier: Modifier = Modifier,
  onAuthorize: () -> Unit,
  onAutomaticChanged: (Boolean) -> Unit,
  onNetworkChanged: (BackupNetworkRequirement) -> Unit,
  onNotifyChanged: (Boolean) -> Unit,
  onBackUpNow: () -> Unit,
  onDisconnect: () -> Unit,
) {
  var showDisconnectConfirmation by rememberSaveable { mutableStateOf(false) }
  LazyColumn(
    modifier = modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(
        copy.serviceState,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.heading(),
      )
      Text(copy.authorizationLabel(state.authorization))
      if (state.authorization !is BackupAuthorizationState.Authorized) {
        Button(onClick = onAuthorize, modifier = Modifier.padding(top = 8.dp)) {
          Text(copy.connect)
        }
      }
    }
    item {
      ToggleSetting(
        title = copy.automaticBackup,
        description = copy.automaticBackupDescription,
        checked = state.settings.automaticBackupEnabled,
        enabled = !state.isUpdatingSettings,
        onCheckedChange = onAutomaticChanged,
      )
      if (state.settings.automaticBackupEnabled) {
        Text(copy.inexactSchedule, style = MaterialTheme.typography.bodySmall)
      }
    }
    item {
      ToggleSetting(
        title = copy.unmeteredOnly,
        description = copy.unmeteredDescription,
        checked = state.settings.networkRequirement == BackupNetworkRequirement.UNMETERED,
        enabled = !state.isUpdatingSettings,
        onCheckedChange = {
          onNetworkChanged(
            if (it) BackupNetworkRequirement.UNMETERED else BackupNetworkRequirement.CONNECTED
          )
        },
      )
    }
    item {
      ToggleSetting(
        title = copy.notifyAfterSuccess,
        description = copy.notifyDescription,
        checked = state.settings.notifyAfterSuccess,
        enabled = !state.isUpdatingSettings,
        onCheckedChange = onNotifyChanged,
      )
    }
    item {
      Button(onClick = onBackUpNow, modifier = Modifier.fillMaxWidth()) { Text(copy.backUpNow) }
      state.manualAttemptStatus?.let { Text(copy.manualStatus(it)) }
    }
    item {
      Text(
        copy.history,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.heading(),
      )
    }
    if (state.attempts.isEmpty()) {
      item { Text(copy.noHistory) }
    } else {
      items(state.attempts, key = { it.id }) { attempt ->
        Card(Modifier.fillMaxWidth()) {
          Text(copy.attemptSummary(attempt), Modifier.padding(12.dp))
        }
      }
    }
    state.error?.let { error ->
      item { Text(copy.error(error), color = MaterialTheme.colorScheme.error) }
    }
    if (state.authorization is BackupAuthorizationState.Authorized) {
      item { TextButton(onClick = { showDisconnectConfirmation = true }) { Text(copy.disconnect) } }
    }
  }

  if (showDisconnectConfirmation) {
    AlertDialog(
      onDismissRequest = { showDisconnectConfirmation = false },
      title = { Text(copy.disconnectConfirmationTitle, modifier = Modifier.heading()) },
      text = { Text(copy.disconnectConfirmationMessage) },
      confirmButton = {
        Button(
          onClick = {
            showDisconnectConfirmation = false
            onDisconnect()
          }
        ) {
          Text(copy.confirmDisconnect)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDisconnectConfirmation = false }) { Text(copy.cancel) }
      },
    )
  }
}

@Composable
private fun ToggleSetting(
  title: String,
  description: String,
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .toggleable(
          value = checked,
          enabled = enabled,
          role = Role.Switch,
          onValueChange = onCheckedChange,
        )
        .semantics { contentDescription = title }
        .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyLarge)
      Text(description, style = MaterialTheme.typography.bodySmall)
    }
    Switch(checked = checked, onCheckedChange = null, enabled = enabled)
  }
}

private fun Modifier.heading(): Modifier = semantics { heading() }
