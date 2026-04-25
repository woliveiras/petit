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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.TransferState
import com.woliveiras.petit.presentation.components.PetitTopAppBar

@Composable
fun TransferScreen(onNavigateBack: () -> Unit, viewModel: TransferViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
        onNavigateBack = {
          viewModel.disconnect()
          onNavigateBack()
        },
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      if (uiState.mergeResult != null) {
        MergeCompleteContent(result = uiState.mergeResult!!, onDone = onNavigateBack)
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
            )
          }
          is TransferState.Receiving -> {
            ProgressContent(
              label = stringResource(R.string.family_group_receiving),
              bytesTransferred = state.bytesTransferred,
              totalBytes = state.totalBytes,
            )
          }
          is TransferState.Complete -> {
            TransferCompleteContent(
              onMerge = { viewModel.mergeReceivedData(replace = false) },
              onReplace = { viewModel.mergeReceivedData(replace = true) },
              isMerging = uiState.isMerging,
            )
          }
          is TransferState.Error -> {
            ErrorTransferContent(message = state.message, onBack = onNavigateBack)
          }
        }
      }
    }
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
private fun ProgressContent(label: String, bytesTransferred: Long, totalBytes: Long) {
  CircularProgressIndicator(modifier = Modifier.size(64.dp))

  Spacer(modifier = Modifier.height(24.dp))

  Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

  Spacer(modifier = Modifier.height(16.dp))

  if (totalBytes > 0) {
    LinearProgressIndicator(
      progress = { bytesTransferred.toFloat() / totalBytes.toFloat() },
      modifier = Modifier.fillMaxWidth(),
    )
  } else {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
    style = MaterialTheme.typography.headlineSmall,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
  )

  Spacer(modifier = Modifier.height(16.dp))

  Text(
    text =
      stringResource(R.string.family_group_merge_summary, result.totalAdded, result.totalUpdated),
    style = MaterialTheme.typography.bodyMedium,
    textAlign = TextAlign.Center,
  )

  Spacer(modifier = Modifier.height(24.dp))

  Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(R.string.action_done))
  }
}

@Composable
private fun ErrorTransferContent(message: String, onBack: () -> Unit) {
  Icon(
    imageVector = Icons.Default.Error,
    contentDescription = null,
    modifier = Modifier.size(64.dp),
    tint = MaterialTheme.colorScheme.error,
  )

  Spacer(modifier = Modifier.height(24.dp))

  Text(
    text = message,
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.error,
    textAlign = TextAlign.Center,
  )

  Spacer(modifier = Modifier.height(24.dp))

  Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(R.string.action_back))
  }
}
