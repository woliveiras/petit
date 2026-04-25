package com.woliveiras.petit.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woliveiras.petit.R

/** Data class representing a Speed Dial action item. */
data class SpeedDialItem(val icon: ImageVector, val label: String, val onClick: () -> Unit)

/**
 * Speed Dial FAB component that expands to show multiple action options.
 *
 * @param expanded Whether the speed dial is currently expanded.
 * @param onExpandedChange Callback when expansion state changes.
 * @param items List of action items to show when expanded.
 * @param modifier Modifier for the container.
 */
@Composable
fun SpeedDialFab(
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  items: List<SpeedDialItem>,
  modifier: Modifier = Modifier,
) {
  val rotation by
    animateFloatAsState(targetValue = if (expanded) 45f else 0f, label = "fab_rotation")

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.End,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Expanded items
    AnimatedVisibility(
      visible = expanded,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut(),
    ) {
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items.forEach { item ->
          SpeedDialItemRow(
            item = item,
            onClick = {
              onExpandedChange(false)
              item.onClick()
            },
          )
        }
      }
    }

    // Main FAB
    FloatingActionButton(onClick = { onExpandedChange(!expanded) }) {
      Icon(
        imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
        contentDescription =
          if (expanded) stringResource(R.string.action_close)
          else stringResource(R.string.action_add),
        modifier = Modifier.rotate(rotation),
      )
    }
  }
}

@Composable
private fun SpeedDialItemRow(item: SpeedDialItem, onClick: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    // Label
    Surface(
      shape = MaterialTheme.shapes.small,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      shadowElevation = 2.dp,
    ) {
      Text(
        text = item.label,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    // Mini FAB
    SmallFloatingActionButton(
      onClick = onClick,
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
      Icon(
        imageVector = item.icon,
        contentDescription = item.label,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}
