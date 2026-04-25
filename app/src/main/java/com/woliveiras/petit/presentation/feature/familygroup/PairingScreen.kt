package com.woliveiras.petit.presentation.feature.familygroup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.PairingState
import com.woliveiras.petit.presentation.components.PetitTopAppBar

private fun nearbyPermissions(): Array<String> =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    arrayOf(
      Manifest.permission.BLUETOOTH_ADVERTISE,
      Manifest.permission.BLUETOOTH_CONNECT,
      Manifest.permission.BLUETOOTH_SCAN,
      Manifest.permission.NEARBY_WIFI_DEVICES,
    )
  } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
      Manifest.permission.BLUETOOTH_ADVERTISE,
      Manifest.permission.BLUETOOTH_CONNECT,
      Manifest.permission.BLUETOOTH_SCAN,
      Manifest.permission.ACCESS_FINE_LOCATION,
    )
  } else {
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
  }

@Composable
fun PairingScreen(
  onNavigateBack: () -> Unit,
  onPairingComplete: () -> Unit,
  viewModel: PairingViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  // Track which action to perform after permissions are granted
  var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

  val permissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
      val allGranted = results.values.all { it }
      if (allGranted) {
        pendingAction?.invoke()
      } else {
        viewModel.onPermissionDenied()
      }
      pendingAction = null
    }

  fun requestPermissionsThen(action: () -> Unit) {
    pendingAction = action
    permissionLauncher.launch(nearbyPermissions())
  }

  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = { Text(stringResource(R.string.family_group_pair_device)) },
        onNavigateBack = {
          viewModel.cancel()
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
      when (val state = uiState.pairingState) {
        is PairingState.Idle -> {
          IdleContent(
            onCreateGroup = { requestPermissionsThen { viewModel.startAdvertising() } },
            onJoinGroup = { requestPermissionsThen { viewModel.startDiscovery() } },
          )
        }
        is PairingState.WaitingForConnection -> {
          WaitingContent(
            code = state.code,
            isDiscovering = !uiState.isCreatingGroup,
            onCancel = viewModel::cancel,
          )
        }
        is PairingState.ConnectionRequested -> {
          ConnectionRequestedContent(
            deviceName = state.deviceName,
            onAccept = { viewModel.acceptConnection(state.endpointId) },
            onReject = { viewModel.rejectConnection(state.endpointId) },
          )
        }
        is PairingState.Paired -> {
          PairedContent(deviceName = state.deviceName, onContinue = onPairingComplete)
        }
        is PairingState.Error -> {
          ErrorContent(message = state.message, onRetry = viewModel::cancel)
        }
      }
    }
  }
}

@Composable
private fun IdleContent(onCreateGroup: () -> Unit, onJoinGroup: () -> Unit) {
  Icon(
    imageVector = Icons.Default.DevicesOther,
    contentDescription = null,
    modifier = Modifier.size(64.dp),
    tint = MaterialTheme.colorScheme.primary,
  )

  Spacer(modifier = Modifier.height(24.dp))

  Text(
    text = stringResource(R.string.family_group_pair_device),
    style = MaterialTheme.typography.headlineSmall,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
  )

  Spacer(modifier = Modifier.height(8.dp))

  Text(
    text = stringResource(R.string.family_group_onboarding_description),
    style = MaterialTheme.typography.bodyMedium,
    textAlign = TextAlign.Center,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )

  Spacer(modifier = Modifier.height(32.dp))

  Button(onClick = onCreateGroup, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(R.string.family_group_pair_device))
  }

  Spacer(modifier = Modifier.height(12.dp))

  OutlinedButton(onClick = onJoinGroup, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(R.string.family_group_join))
  }
}

@Composable
private fun WaitingContent(code: String, isDiscovering: Boolean = false, onCancel: () -> Unit) {
  CircularProgressIndicator(modifier = Modifier.size(64.dp))

  Spacer(modifier = Modifier.height(24.dp))

  Text(
    text = stringResource(R.string.family_group_waiting_connection),
    style = MaterialTheme.typography.bodyLarge,
    textAlign = TextAlign.Center,
  )

  if (!isDiscovering && code.isNotEmpty()) {
    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = stringResource(R.string.family_group_pairing_code),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Text(
      text = code,
      style = MaterialTheme.typography.displaySmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
    )
  } else {
    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = stringResource(R.string.family_group_searching_devices),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }

  Spacer(modifier = Modifier.height(24.dp))

  TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
}

@Composable
private fun ConnectionRequestedContent(
  deviceName: String,
  onAccept: () -> Unit,
  onReject: () -> Unit,
) {
  Icon(
    imageVector = Icons.Default.DevicesOther,
    contentDescription = null,
    modifier = Modifier.size(64.dp),
    tint = MaterialTheme.colorScheme.primary,
  )

  Spacer(modifier = Modifier.height(24.dp))

  Text(
    text = stringResource(R.string.family_group_connection_request, deviceName),
    style = MaterialTheme.typography.bodyLarge,
    textAlign = TextAlign.Center,
  )

  Spacer(modifier = Modifier.height(24.dp))

  Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(R.string.action_accept))
  }

  Spacer(modifier = Modifier.height(8.dp))

  OutlinedButton(onClick = onReject, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(R.string.action_reject))
  }
}

@Composable
private fun PairedContent(deviceName: String, onContinue: () -> Unit) {
  Icon(
    imageVector = Icons.Default.CheckCircle,
    contentDescription = null,
    modifier = Modifier.size(64.dp),
    tint = MaterialTheme.colorScheme.primary,
  )

  Spacer(modifier = Modifier.height(24.dp))

  Text(
    text = stringResource(R.string.family_group_paired_success),
    style = MaterialTheme.typography.headlineSmall,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
  )

  Spacer(modifier = Modifier.height(8.dp))

  Text(
    text = deviceName,
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
  )

  Spacer(modifier = Modifier.height(32.dp))

  Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(R.string.action_continue))
  }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
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

  Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(R.string.action_retry))
  }
}
