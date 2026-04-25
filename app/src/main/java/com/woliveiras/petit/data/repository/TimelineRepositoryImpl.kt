package com.woliveiras.petit.data.repository

import com.woliveiras.petit.data.local.dao.RawTimelineEvent
import com.woliveiras.petit.data.local.dao.TimelineDao
import com.woliveiras.petit.domain.model.TimelineEvent
import com.woliveiras.petit.domain.model.TimelineEventType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementation of TimelineRepository using Room database. */
@Singleton
class TimelineRepositoryImpl @Inject constructor(private val timelineDao: TimelineDao) :
  TimelineRepository {

  override fun getRecentActivity(): Flow<List<TimelineEvent>> {
    return timelineDao.getRecentActivity().map { rawEvents -> rawEvents.map { it.toDomain() } }
  }

  override fun getUpcomingEvents(daysAhead: Int): Flow<List<TimelineEvent>> {
    val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val futureLimit =
      LocalDate.now()
        .plusDays(daysAhead.toLong())
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    return timelineDao.getUpcomingEvents(today, futureLimit).map { rawEvents ->
      rawEvents.map { it.toDomain() }
    }
  }

  override fun getOverdueEvents(): Flow<List<TimelineEvent>> {
    val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    return timelineDao.getOverdueEvents(today).map { rawEvents -> rawEvents.map { it.toDomain() } }
  }

  override fun getAllActivity(startDate: LocalDate, endDate: LocalDate): Flow<List<TimelineEvent>> {
    val start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val end =
      endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

    return timelineDao.getAllActivity(start, end).map { rawEvents ->
      rawEvents.map { it.toDomain() }
    }
  }

  private fun RawTimelineEvent.toDomain(): TimelineEvent {
    val eventType =
      try {
        TimelineEventType.valueOf(this.eventType)
      } catch (_: IllegalArgumentException) {
        TimelineEventType.WEIGHT // fallback
      }

    val eventDate =
      Instant.ofEpochMilli(this.eventDate).atZone(ZoneId.systemDefault()).toLocalDate()

    return TimelineEvent(
      id = this.id,
      eventType = eventType,
      petId = this.petId,
      petName = this.petName,
      petPhotoUri = this.petPhotoUri,
      eventDate = eventDate,
      title = this.title,
      subtitle = this.subtitle,
      isFuture = this.isFuture == 1,
    )
  }
}
