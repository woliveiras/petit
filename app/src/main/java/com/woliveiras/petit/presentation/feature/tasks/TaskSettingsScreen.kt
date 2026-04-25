package com.woliveiras.petit.presentation.feature.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R
import com.woliveiras.petit.presentation.components.PetitTopAppBar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSettingsScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: TaskSettingsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  // Time picker dialog
  if (uiState.showTimePicker) {
    val timePickerState =
      rememberTimePickerState(
        initialHour = uiState.notificationHour,
        initialMinute = uiState.notificationMinute,
        is24Hour = true,
      )

    AlertDialog(
      onDismissRequest = viewModel::hideTimePicker,
      title = { Text(stringResource(R.string.task_settings_notification_time)) },
      text = { TimePicker(state = timePickerState) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.updateNotificationTime(timePickerState.hour, timePickerState.minute)
          }
        ) {
          Text(stringResource(R.string.action_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = viewModel::hideTimePicker) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  Scaffold(
    topBar = {
      PetitTopAppBar(
        title = { Text(stringResource(R.string.task_settings_title)) },
        onNavigateBack = onNavigateBack,
      )
    },
    modifier = modifier,
  ) { padding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // General settings section
      SectionHeader(stringResource(R.string.task_settings_section_general))

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
      ) {
        ListItem(
          modifier = Modifier.clickable { viewModel.showTimePicker() },
          colors =
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          leadingContent = {
            Icon(
              Icons.Default.AccessTime,
              contentDescription = stringResource(R.string.cd_icon_task),
              tint = MaterialTheme.colorScheme.primary,
            )
          },
          headlineContent = { Text(stringResource(R.string.task_settings_notification_time)) },
          supportingContent = { Text(uiState.notificationTimeFormatted) },
        )
      }

      // Vaccination tasks
      SectionHeader(stringResource(R.string.task_settings_section_vaccination))

      TaskSettingsCard(
        icon = Icons.Default.Vaccines,
        title = stringResource(R.string.task_settings_vaccination_title),
        description = stringResource(R.string.task_settings_vaccination_description),
        enabled = uiState.vaccinationEnabled,
        onToggle = viewModel::toggleVaccinationTasks,
        daysBefore = uiState.vaccinationDaysBefore,
        onDaysChange = viewModel::updateVaccinationDaysBefore,
        daysLabel = stringResource(R.string.task_settings_days_before),
        minDays = 1,
        maxDays = 30,
      )

      // Deworming tasks
      SectionHeader(stringResource(R.string.task_settings_section_deworming))

      TaskSettingsCard(
        icon = Icons.Default.Healing,
        title = stringResource(R.string.task_settings_deworming_title),
        description = stringResource(R.string.task_settings_deworming_description),
        enabled = uiState.dewormingEnabled,
        onToggle = viewModel::toggleDewormingTasks,
        daysBefore = uiState.dewormingDaysBefore,
        onDaysChange = viewModel::updateDewormingDaysBefore,
        daysLabel = stringResource(R.string.task_settings_days_before),
        minDays = 1,
        maxDays = 30,
      )

      // Weight tasks
      SectionHeader(stringResource(R.string.task_settings_section_weight))

      TaskSettingsCard(
        icon = Icons.Default.MonitorWeight,
        title = stringResource(R.string.task_settings_weight_title),
        description = stringResource(R.string.task_settings_weight_description),
        enabled = uiState.weightEnabled,
        onToggle = viewModel::toggleWeightTasks,
        daysBefore = uiState.weightIntervalDays,
        onDaysChange = viewModel::updateWeightIntervalDays,
        daysLabel = stringResource(R.string.task_settings_interval_days),
        minDays = 7,
        maxDays = 90,
      )
    }
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
  )
}

@Composable
private fun TaskSettingsCard(
  icon: ImageVector,
  title: String,
  description: String,
  enabled: Boolean,
  onToggle: (Boolean) -> Unit,
  daysBefore: Int,
  onDaysChange: (Int) -> Unit,
  daysLabel: String,
  minDays: Int,
  maxDays: Int,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint =
            if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
          )
          Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
      }

      if (enabled) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text =
            "$daysLabel: " +
              pluralStringResource(R.plurals.task_settings_days, daysBefore, daysBefore),
          style = MaterialTheme.typography.bodyMedium,
        )

        Slider(
          value = daysBefore.toFloat(),
          onValueChange = { onDaysChange(it.roundToInt()) },
          valueRange = minDays.toFloat()..maxDays.toFloat(),
          steps = maxDays - minDays - 1,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "$minDays",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
          )
          Spacer(modifier = Modifier.weight(1f))
          Text(
            text = "$maxDays",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
          )
        }
      }
    }
  }
}
