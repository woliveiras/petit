package com.woliveiras.petit.presentation.feature.familygroup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.FamilyGroupInfo

/** Inline section shown in the Profile/Settings screen displaying family group status. */
@Composable
fun FamilyGroupSection(
  familyGroupInfo: FamilyGroupInfo?,
  lastSyncText: String?,
  onPairDevice: () -> Unit,
  onJoinGroup: () -> Unit,
  onManageGroup: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = RoundedCornerShape(16.dp),
  ) {
    if (familyGroupInfo != null) {
      PairedContent(
        familyGroupInfo = familyGroupInfo,
        lastSyncText = lastSyncText,
        onManageGroup = onManageGroup,
      )
    } else {
      OnboardingContent(onPairDevice = onPairDevice, onJoinGroup = onJoinGroup)
    }
  }
}

@Composable
private fun PairedContent(
  familyGroupInfo: FamilyGroupInfo,
  lastSyncText: String?,
  onManageGroup: () -> Unit,
) {
  Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Default.DevicesOther,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
      Column(modifier = Modifier.weight(1f)) {
        val remoteMembers = familyGroupInfo.members.filter { !it.isLocalDevice }
        val remoteName =
          remoteMembers.firstOrNull()?.deviceName ?: stringResource(R.string.family_group_title)
        Text(
          text = remoteName,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
        )
        if (lastSyncText != null) {
          Text(
            text = lastSyncText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    ListItem(
      modifier = Modifier.clickable(onClick = onManageGroup),
      colors =
        ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      headlineContent = {
        Text(
          text = stringResource(R.string.family_group_manage),
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium,
        )
      },
      trailingContent = {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      },
    )
  }
}

@Composable
private fun OnboardingContent(onPairDevice: () -> Unit, onJoinGroup: () -> Unit) {
  Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Icon(
      imageVector = Icons.Default.SyncAlt,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
    )

    Text(
      text = stringResource(R.string.family_group_onboarding_description),
      style = MaterialTheme.typography.bodyMedium,
    )

    Text(
      text = stringResource(R.string.family_group_no_internet_hint),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(onClick = onPairDevice, modifier = Modifier.weight(1f)) {
        Icon(
          imageVector = Icons.Default.DevicesOther,
          contentDescription = null,
          modifier = Modifier.padding(end = 4.dp),
        )
        Text(stringResource(R.string.family_group_pair_device))
      }

      OutlinedButton(onClick = onJoinGroup, modifier = Modifier.weight(1f)) {
        Icon(
          imageVector = Icons.Default.GroupAdd,
          contentDescription = null,
          modifier = Modifier.padding(end = 4.dp),
        )
        Text(stringResource(R.string.family_group_join))
      }
    }
  }
}
