package com.woliveiras.petit.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woliveiras.petit.R
import com.woliveiras.petit.domain.model.TimelineEvent

/**
 * A section displaying a list of timeline events with a vertical timeline. Each event has a dot on
 * the left connected by a vertical line, with the event card to the right. The first dot is larger
 * and filled, subsequent dots are smaller and outlined.
 */
@Composable
fun TimelineSection(
  modifier: Modifier = Modifier,
  title: String,
  events: List<TimelineEvent>,
  onEventClick: ((TimelineEvent) -> Unit)? = null,
) {
  if (events.isEmpty()) return

  Column(modifier = modifier.fillMaxWidth()) {
    // Section title
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 25.dp),
    )

    // Timeline items with dots (designed for small lists; use LazyColumn for unbounded lists)
    events.forEachIndexed { index, event ->
      val isFirst = index == 0
      val isLast = index == events.lastIndex

      TimelineItem(
        event = event,
        isFirst = isFirst,
        isLast = isLast,
        onClick = onEventClick?.let { { it(event) } },
      )
    }
  }
}

@Composable
private fun TimelineItem(
  event: TimelineEvent,
  isFirst: Boolean,
  isLast: Boolean,
  onClick: (() -> Unit)?,
) {
  val dotColor = MaterialTheme.colorScheme.primary
  val lineColor = MaterialTheme.colorScheme.outlineVariant
  val dotRadius: Dp = if (isFirst) 8.dp else 6.dp
  val isFilled = isFirst

  // IntrinsicSize.Min lets the Canvas match the card's height for correct line drawing
  Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
    // Timeline rail: dot + vertical line
    TimelineRail(
      dotColor = dotColor,
      lineColor = lineColor,
      dotRadius = dotRadius,
      isFilled = isFilled,
      showLineAbove = !isFirst,
      showLineBelow = !isLast,
      modifier = Modifier.fillMaxHeight().width(32.dp),
    )

    Spacer(modifier = Modifier.width(8.dp))

    // Event card — bottom padding on non-last items keeps spacing without breaking the rail line
    Box(modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 12.dp)) {
      TimelineEventCard(event = event, onClick = onClick)
    }
  }
}

/**
 * Draws the vertical timeline rail: a dot (filled or outlined) with optional vertical lines above
 * and below connecting to adjacent items.
 */
@Composable
private fun TimelineRail(
  dotColor: Color,
  lineColor: Color,
  dotRadius: Dp,
  isFilled: Boolean,
  showLineAbove: Boolean,
  showLineBelow: Boolean,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val centerX = size.width / 2f
    val dotRadiusPx = dotRadius.toPx()
    // Anchor the dot near the card's top corner to keep the rail visually aligned with each card.
    val dotCenterY = 12.dp.toPx()
    val lineWidthPx = 2.dp.toPx()

    // Line above (from top to dot)
    if (showLineAbove) {
      drawLine(
        color = lineColor,
        start = Offset(centerX, 0f),
        end = Offset(centerX, dotCenterY - dotRadiusPx),
        strokeWidth = lineWidthPx,
      )
    }

    // Line below (from dot to bottom)
    if (showLineBelow) {
      drawLine(
        color = lineColor,
        start = Offset(centerX, dotCenterY + dotRadiusPx),
        end = Offset(centerX, size.height),
        strokeWidth = lineWidthPx,
      )
    }

    // Dot
    if (isFilled) {
      drawCircle(color = dotColor, radius = dotRadiusPx, center = Offset(centerX, dotCenterY))
    } else {
      drawCircle(
        color = dotColor,
        radius = dotRadiusPx,
        center = Offset(centerX, dotCenterY),
        style = Stroke(width = 2.dp.toPx()),
      )
    }
  }
}

/** Preview-friendly version that takes simple data. */
@Composable
fun TimelineSectionPreview(title: String, eventCount: Int, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 8.dp),
    )

    Text(
      text = stringResource(R.string.timeline_event_count, eventCount),
      modifier = Modifier.padding(16.dp),
    )
  }
}
