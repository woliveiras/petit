package com.woliveiras.petit.data.repository

import com.woliveiras.petit.domain.model.TimelineEvent
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Repository interface for timeline events. */
interface TimelineRepository {
  /** Get recent past activity events. Returns the last 5 events of each type per pet. */
  fun getRecentActivity(): Flow<List<TimelineEvent>>

  /** Get upcoming scheduled events within the next N days. */
  fun getUpcomingEvents(daysAhead: Int = 30): Flow<List<TimelineEvent>>

  /** Get overdue events (past due dates that weren't completed). */
  fun getOverdueEvents(): Flow<List<TimelineEvent>>

  /** Get all past activity events within a date range, without per-pet limits. */
  fun getAllActivity(startDate: LocalDate, endDate: LocalDate): Flow<List<TimelineEvent>>
}
