package com.woliveiras.petit.presentation.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.TimelineRepository
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.TimelineEvent
import com.woliveiras.petit.domain.model.TimelineEventType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Date filter options for the activity timeline. */
enum class TimelineFilter {
  ALL,
  DAYS_5,
  DAYS_10,
  DAYS_15,
  CUSTOM,
}

/** Event type filter — only past activity types (not scheduled/due). */
enum class EventTypeFilter {
  WEIGHT,
  VACCINATION,
  DEWORMING,
}

/** UI state for the Activity Timeline screen. */
data class ActivityTimelineUiState(
  val events: List<TimelineEvent> = emptyList(),
  val pets: List<Pet> = emptyList(),
  val filter: TimelineFilter = TimelineFilter.ALL,
  val customDate: LocalDate? = null,
  val selectedCatId: String? = null,
  val selectedEventTypes: Set<EventTypeFilter> = emptySet(),
  val isLoading: Boolean = true,
)

private data class FilterParams(
  val filter: TimelineFilter,
  val customDate: LocalDate?,
  val petId: String?,
  val eventTypes: Set<EventTypeFilter>,
)

@HiltViewModel
class ActivityTimelineViewModel
@Inject
constructor(
  private val timelineRepository: TimelineRepository,
  private val petRepository: PetRepository,
) : ViewModel() {

  private val _filter = MutableStateFlow(TimelineFilter.ALL)
  private val _customDate = MutableStateFlow<LocalDate?>(null)
  private val _selectedCatId = MutableStateFlow<String?>(null)
  private val _selectedEventTypes = MutableStateFlow<Set<EventTypeFilter>>(emptySet())

  @OptIn(ExperimentalCoroutinesApi::class)
  val uiState: StateFlow<ActivityTimelineUiState> =
    combine(_filter, _customDate, _selectedCatId, _selectedEventTypes) {
        filter,
        customDate,
        petId,
        eventTypes ->
        FilterParams(filter, customDate, petId, eventTypes)
      }
      .flatMapLatest { params ->
        val today = LocalDate.now()
        val (startDate, endDate) =
          when (params.filter) {
            TimelineFilter.ALL -> today.minusYears(1) to today
            TimelineFilter.DAYS_5 -> today.minusDays(5) to today
            TimelineFilter.DAYS_10 -> today.minusDays(10) to today
            TimelineFilter.DAYS_15 -> today.minusDays(15) to today
            TimelineFilter.CUSTOM -> {
              val date = params.customDate ?: today
              date to date
            }
          }

        val timelineEventTypes =
          if (params.eventTypes.isEmpty()) {
            null
          } else {
            params.eventTypes.map {
              when (it) {
                EventTypeFilter.WEIGHT -> TimelineEventType.WEIGHT
                EventTypeFilter.VACCINATION -> TimelineEventType.VACCINATION
                EventTypeFilter.DEWORMING -> TimelineEventType.DEWORMING
              }
            }
          }

        combine(
          timelineRepository.getAllActivity(startDate, endDate),
          petRepository.getAllPets(),
        ) { events, pets ->
          val filtered =
            events
              .filter { event -> params.petId == null || event.petId == params.petId }
              .filter { event ->
                timelineEventTypes == null || event.eventType in timelineEventTypes
              }

          ActivityTimelineUiState(
            events = filtered,
            pets = pets,
            filter = params.filter,
            customDate = params.customDate,
            selectedCatId = params.petId,
            selectedEventTypes = params.eventTypes,
            isLoading = false,
          )
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActivityTimelineUiState(),
      )

  fun setFilter(filter: TimelineFilter) {
    _filter.value = filter
  }

  fun setCustomDate(date: LocalDate) {
    _customDate.value = date
    _filter.value = TimelineFilter.CUSTOM
  }

  fun setSelectedPet(petId: String?) {
    _selectedCatId.value = petId
  }

  fun toggleEventType(type: EventTypeFilter) {
    _selectedEventTypes.value =
      _selectedEventTypes.value.let { current ->
        if (type in current) current - type else current + type
      }
  }
}
