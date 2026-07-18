package com.woliveiras.petit.presentation.feature.familygroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.TransferError
import com.woliveiras.petit.domain.model.TransferState
import com.woliveiras.petit.presentation.components.PetitTopAppBar

@Composable
fun TransferScreen(onNavigateBack: () -> Unit, viewModel: TransferViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val disconnectAndNavigateBack = {
    viewModel.disconnect()
    onNavigateBack()
  }

  if (uiState.showReplaceConfirmation) {
    AlertDialog(
      onDismissRequest = viewModel::dismissReplace,
      title = { Text(stringResource(R.string.family_group_replace_confirm_title)) },
      text = { Text(stringResource(R.string.family_group_replace_confirm_message)) },
      confirmButton = {
        Button(onClick = viewModel::confirmReplace) {
          Text(stringResource(R.string.action_confirm))
        }
      },
      dismissButton = {
        OutlinedButton(onClick = viewModel::dismissReplace) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = {
          Text(
            stringResource(
              if (uiState.isSending) R.string.family_group_send_data
              else R.string.family_group_receive_data
            )
          )
        },
        onNavigateBack = disconnectAndNavigateBack,
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      if (uiState.mergeResult != null) {
        MergeCompleteContent(result = uiState.mergeResult!!, onDone = disconnectAndNavigateBack)
      } else {
        when (val state = uiState.transferState) {
          is TransferState.Idle -> {
            if (uiState.isSending) {
              SendingIdleContent()
            } else {
              ReceivingIdleContent()
            }
          }
          is TransferState.Sending -> {
            ProgressContent(
              label = stringResource(R.string.family_group_sending),
              bytesTransferred = state.bytesTransferred,
              totalBytes = state.totalBytes,
              onCancel = viewModel::cancelTransfer,
            )
          }
          is TransferState.Receiving -> {
            ProgressContent(
              label = stringResource(R.string.family_group_receiving),
              bytesTransferred = state.bytesTransferred,
              totalBytes = state.totalBytes,
              onCancel = viewModel::cancelTransfer,
            )
          }
          is TransferState.Sent -> {
            SentContent(onDone = disconnectAndNavigateBack)
          }
          is TransferState.Complete -> {
            TransferCompleteContent(
              onMerge = { viewModel.mergeReceivedData(replace = false) },
              onReplace = viewModel::requestReplace,
              isMerging = uiState.isMerging,
            )
          }
          is TransferState.Error -> {
            ErrorTransferContent(
              message = stringResource(state.reason.messageResource()),
              onRetry = if (uiState.isSending) viewModel::retry else null,
              onBack = disconnectAndNavigateBack,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SentContent(onDone: () -> Unit) {
  Icon(
    imageVector = Icons.Default.CheckCircle,
    contentDescription = null,
    modifier = Modifier.size(64.dp),
    tint = MaterialTheme.colorScheme.primary,
  )
  Spacer(modifier = Modifier.height(24.dp))
  Text(
    text = stringResource(R.string.family_group_data_sent),
    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    style = MaterialTheme.typography.headlineSmall,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
  )
  Spacer(modifier = Modifier.height(24.dp))
  Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(R.string.action_done))
  }
}

@Composable
private fun SendingIdleContent() {
  Icon(
    imageVector = Icons.Default.Upload,
    contentDescription = null,
    modifier = Modifier.size(64.dp),
    tint = MaterialTheme.colorScheme.primary,
  )

  Spacer(modifier = Modifier.height(24.dp))

  Text(
    text = stringResource(R.string.family_group_preparing_send),
    style = MaterialTheme.typography.bodyLarge,
    textAlign = TextAlign.Center,
  )

  Spacer(modifier = Modifier.height(16.dp))

  CircularProgressIndicator()
}

@Composable
private fun ReceivingIdleContent() {
  Icon(
    imageVector = Icons.Default.Download,
    contentDescription = null,
    modifier = Modifier.size(64.dp),
    tint = MaterialTheme.colorScheme.primary,
  )

  Spacer(modifier = Modifier.height(24.dp))

  Text(
    text = stringResource(R.string.family_group_waiting_data),
    style = MaterialTheme.typography.bodyLarge,
    textAlign = TextAlign.Center,
  )

  Spacer(modifier = Modifier.height(8.dp))

  Text(
    text = stringResource(R.string.family_group_waiting_data_hint),
    style = MaterialTheme.typography.bodySmall,
    textAlign = TextAlign.Center,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )

  Spacer(modifier = Modifier.height(16.dp))

  CircularProgressIndicator()
}

@Composable
private fun ProgressContent(
  label: String,
  bytesTransferred: Long,
  totalBytes: Long,
  onCancel: () -> Unit,
) {
  CircularProgressIndicator(modifier = Modifier.size(64.dp))

  Spacer(modifier = Modifier.height(24.dp))

  Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

  Spacer(modifier = Modifier.height(16.dp))

  if (totalBytes > 0) {
    LinearProgressIndicator(
      progress = { bytesTransferred.toFloat() / totalBytes.toFloat() },
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    val percentage = ((bytesTransferred * 100) / totalBytes).coerceIn(0, 100)
    Text(
      text =
        stringResource(
          R.string.family_group_transfer_progress,
          bytesTransferred,
          totalBytes,
          percentage,
        ),
      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
      style = MaterialTheme.typography.bodySmall,
    )
  } else {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
  }
  Spacer(modifier = Modifier.height(24.dp))
  OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(R.string.action_cancel))
  }
}

@Composable
private fun TransferCompleteContent(
  onMerge: () -> Unit,
  onReplace: () -> Unit,
  isMerging: Boolean,
) {
  Icon(
    imageVector = Icons.Default.CheckCircle,
    contentDescription = null,
    modifier = Modifier.size(64.dp),
    tint = MaterialTheme.colorScheme.primary,
  )

  Spacer(modifier = Modifier.height(24.dp))

  Text(
    text = stringResource(R.string.family_group_transfer_complete),
    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    style = MaterialTheme.typography.headlineSmall,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
  )

  Spacer(modifier = Modifier.height(8.dp))

  Text(
    text = stringResource(R.string.family_group_choose_action),
    style = MaterialTheme.typography.bodyMedium,
    textAlign = TextAlign.Center,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )

  Spacer(modifier = Modifier.height(24.dp))

  if (isMerging) {
    CircularProgressIndicator()
  } else {
    Button(onClick = onMerge, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.family_group_merge))
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(onClick = onReplace, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.family_group_replace))
    }
  }
}

@Composable
private fun MergeCompleteContent(
  result: com.woliveiras.petit.domain.model.MergeResult,
  onDone: () -> Unit,
) {
  Icon(
    imageVector = Icons.Default.CheckCircle,
    contentDescription = null,
    modifier = Modifier.size(64.dp),
    tint = MaterialTheme.colorScheme.primary,
  )

  Spacer(modifier = Modifier.height(24.dp))

  Text(
    text = stringResource(R.string.family_group_merge_complete),
    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    style = MaterialTheme.typography.headlineSmall,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
  )

  Spacer(modifier = Modifier.height(16.dp))

  Text(
    text =
      stringResource(
        R.string.family_group_merge_summary,
        result.totalAdded,
        result.totalUpdated,
        result.totalRemoved,
      ),
    style = MaterialTheme.typography.bodyMedium,
    textAlign = TextAlign.Center,
  )

  ResultRow(
    R.string.family_group_result_pets,
    result.petsAdded,
    result.petsUpdated,
    result.petsRemoved,
  )
  ResultRow(
    R.string.family_group_result_weights,
    result.weightsAdded,
    result.weightsUpdated,
    result.weightsRemoved,
  )
  ResultRow(
    R.string.family_group_result_vaccinations,
    result.vaccinationsAdded,
    result.vaccinationsUpdated,
    result.vaccinationsRemoved,
  )
  ResultRow(
    R.string.family_group_result_dewormings,
    result.dewormingsAdded,
    result.dewormingsUpdated,
    result.dewormingsRemoved,
  )
  ResultRow(
    R.string.family_group_result_tasks,
    result.tasksAdded,
    result.tasksUpdated,
    result.tasksRemoved,
  )

  Spacer(modifier = Modifier.height(24.dp))

  Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(R.string.action_done))
  }
}

@Composable
private fun ResultRow(labelResource: Int, added: Int, updated: Int, removed: Int) {
  Text(
    text =
      stringResource(
        R.string.family_group_result_row,
        stringResource(labelResource),
        added,
        updated,
        removed,
      ),
    style = MaterialTheme.typography.bodySmall,
    textAlign = TextAlign.Center,
  )
}

@Composable
private fun ErrorTransferContent(message: String, onRetry: (() -> Unit)?, onBack: () -> Unit) {
  Icon(
    imageVector = Icons.Default.Error,
    contentDescription = null,
    modifier = Modifier.size(64.dp),
    tint = MaterialTheme.colorScheme.error,
  )

  Spacer(modifier = Modifier.height(24.dp))

  Text(
    text = message,
    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.error,
    textAlign = TextAlign.Center,
  )

  Spacer(modifier = Modifier.height(24.dp))

  if (onRetry != null) {
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.action_retry))
    }
    Spacer(modifier = Modifier.height(8.dp))
  }
  OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(R.string.action_back))
  }
}

private fun TransferError.messageResource(): Int =
  when (this) {
    TransferError.NoConnectedDevice -> R.string.family_group_error_no_endpoint
    TransferError.PayloadTooLarge -> R.string.family_group_error_payload_too_large
    TransferError.TransferFailed -> R.string.family_group_error_transfer_failed
    TransferError.InvalidData -> R.string.family_group_error_invalid_data
    TransferError.ParseFailed -> R.string.family_group_error_parse_failed
    TransferError.UnsupportedPayload -> R.string.family_group_error_unsupported_payload
    TransferError.MergeFailed -> R.string.family_group_error_merge_failed
  }
