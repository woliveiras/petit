package com.woliveiras.petit.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Represents an event in the health timeline. Can be either a past activity or an upcoming
 * scheduled event.
 */
data class TimelineEvent(
  val id: String,
  val eventType: TimelineEventType,
  val petId: String?,
  val petName: String,
  val petPhotoUri: String?,
  val eventDate: LocalDate,
  val title: String,
  val subtitle: String?,
  val isFuture: Boolean,
) {
  /** Days until or since the event. Positive = future, Negative = past, 0 = today */
  val daysFromToday: Long
    get() = ChronoUnit.DAYS.between(LocalDate.now(), eventDate)

  val isOverdue: Boolean
    get() = isFuture && daysFromToday < 0
}

/** Types of timeline events. */
enum class TimelineEventType {
  // Past events (activities)
  WEIGHT,
  VACCINATION,
  DEWORMING,

  // Future events (scheduled)
  VACCINATION_DUE,
  DEWORMING_DUE,
  REMINDER,
}
