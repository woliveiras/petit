package com.woliveiras.petit.presentation.feature.pets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woliveiras.petit.R
import com.woliveiras.petit.presentation.components.PetitTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDeleteConfirmationScreen(
  petId: String,
  onNavigateBack: () -> Unit,
  onPetDeleted: () -> Unit,
  viewModel: PetDeleteConfirmationViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is PetDeleteConfirmationEvent.PetDeleted -> {
          /* Handled by isDeleted state */
        }
        is PetDeleteConfirmationEvent.Error -> {
          snackbarHostState.showSnackbar(event.message)
        }
      }
    }
  }

  if (uiState.isDeleted) {
    DeleteSuccessScreen(petName = uiState.petName, onGoHome = onPetDeleted)
  } else {
    DeleteConfirmationContent(
      uiState = uiState,
      onNavigateBack = onNavigateBack,
      onDelete = { viewModel.deletePet() },
      snackbarHostState = snackbarHostState,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteSuccessScreen(petName: String, onGoHome: () -> Unit) {
  Scaffold { padding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = stringResource(R.string.cd_icon_check),
          modifier = Modifier.size(100.dp),
          tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
          text = stringResource(R.string.pet_delete_success_title, petName),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = stringResource(R.string.pet_delete_success_message),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = onGoHome, modifier = Modifier.fillMaxWidth().height(56.dp)) {
          Text(
            text = stringResource(R.string.pet_delete_go_home),
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteConfirmationContent(
  uiState: PetDeleteConfirmationUiState,
  onNavigateBack: () -> Unit,
  onDelete: () -> Unit,
  snackbarHostState: SnackbarHostState,
) {
  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      PetitTopAppBar(
        title = { Text(stringResource(R.string.pet_delete_title)) },
        onNavigateBack = onNavigateBack,
      )
    },
  ) { padding ->
    if (uiState.isLoading) {
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else if (uiState.pet != null) {
      val context = LocalContext.current
      Column(
        modifier =
          Modifier.fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Pet avatar
        Surface(
          modifier = Modifier.size(120.dp).clip(CircleShape),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Box(contentAlignment = Alignment.Center) {
            if (uiState.pet?.photoUri != null) {
              AsyncImage(
                model =
                  ImageRequest.Builder(context).data(uiState.pet?.photoUri).crossfade(true).build(),
                contentDescription = uiState.pet?.name,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
              )
            } else {
              Text(
                text = uiState.pet?.name?.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pet name
        Text(
          text = uiState.pet?.name ?: "",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Warning card
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          shape = RoundedCornerShape(16.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = stringResource(R.string.cd_icon_warning),
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
              Text(
                text = stringResource(R.string.pet_delete_warning_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = stringResource(R.string.pet_delete_warning_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Records summary
        if (uiState.totalRecordsCount > 0) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
              Text(
                text = stringResource(R.string.pet_delete_records_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )

              Spacer(modifier = Modifier.height(12.dp))

              if (uiState.weightEntriesCount > 0) {
                RecordCountRow(
                  label = stringResource(R.string.pet_delete_weight_records),
                  count = uiState.weightEntriesCount,
                )
              }

              if (uiState.vaccinationEntriesCount > 0) {
                RecordCountRow(
                  label = stringResource(R.string.pet_delete_vaccination_records),
                  count = uiState.vaccinationEntriesCount,
                )
              }

              if (uiState.dewormingEntriesCount > 0) {
                RecordCountRow(
                  label = stringResource(R.string.pet_delete_deworming_records),
                  count = uiState.dewormingEntriesCount,
                )
              }

              HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

              RecordCountRow(
                label = stringResource(R.string.pet_delete_total_records),
                count = uiState.totalRecordsCount,
                isBold = true,
              )
            }
          }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Button(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            enabled = !uiState.isDeleting,
          ) {
            if (uiState.isDeleting) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onError,
              )
            } else {
              Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = stringResource(R.string.cd_icon_delete),
                modifier = Modifier.size(24.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.pet_delete_confirm_button),
                style = MaterialTheme.typography.titleMedium,
              )
            }
          }

          OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !uiState.isDeleting,
          ) {
            Text(
              text = stringResource(R.string.action_cancel),
              style = MaterialTheme.typography.titleMedium,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RecordCountRow(label: String, count: Int, isBold: Boolean = false) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
    )

    Text(
      text = count.toString(),
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
      color = MaterialTheme.colorScheme.error,
    )
  }
}
