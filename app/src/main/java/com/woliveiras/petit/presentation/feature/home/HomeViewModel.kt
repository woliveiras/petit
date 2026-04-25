package com.woliveiras.petit.presentation.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.data.repository.TimelineRepository
import com.woliveiras.petit.data.repository.WeightEntryRepository
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.HealthStatus
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TimelineEvent
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.domain.model.WeightEntry
import com.woliveiras.petit.domain.model.WeightStatus
import com.woliveiras.petit.domain.usecase.GetPetHealthSummaryAction
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** UI State for the Home Dashboard. */
data class HomeUiState(
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val pets: List<PetWithSummary> = emptyList(),
  val tasksDueToday: List<Task> = emptyList(),
  val tasksDueThisWeek: List<Task> = emptyList(),
  val tasksDueThisMonth: List<Task> = emptyList(),
  val nextTasks: List<Task> = emptyList(),
  val hasMoreTasks: Boolean = false,
  val recentActivity: List<TimelineEvent> = emptyList(),
  val upcomingTimeline: List<TimelineEvent> = emptyList(),
  val isEmpty: Boolean = false,
) {
  companion object {
    const val MAX_HOME_TASKS = 5
  }
}

/** Pet with summary information for display. */
data class PetWithSummary(
  val pet: Pet,
  val latestWeight: WeightEntry? = null,
  val weightStatus: WeightStatus = WeightStatus.NO_DATA,
  val overallStatus: HealthStatus = HealthStatus.OK,
  val nextVaccineType: VaccineType? = null,
  val nextVaccinationDate: LocalDate? = null,
  val nextDewormingType: DewormingType? = null,
  val nextDewormingDate: LocalDate? = null,
)

@HiltViewModel
class HomeViewModel
@Inject
constructor(
  private val petRepository: PetRepository,
  private val weightEntryRepository: WeightEntryRepository,
  private val getPetHealthSummary: GetPetHealthSummaryAction,
  private val taskRepository: TaskRepository,
  private val timelineRepository: TimelineRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  private var dashboardJob: Job? = null

  init {
    loadDashboard()
  }

  private fun loadDashboard() {
    dashboardJob?.cancel()
    dashboardJob =
      viewModelScope.launch {
        // Combine cats flow with weight changes trigger and tasks
        combine(
            petRepository.getAllPets(),
            weightEntryRepository.observeWeightChanges(),
            taskRepository.getTasksDueToday(),
            taskRepository.getTasksDueThisWeek(),
            taskRepository.getTasksDueThisMonth(),
          ) { pets, _, tasksToday, tasksThisWeek, tasksThisMonth ->
            DashboardData(pets, tasksToday, tasksThisWeek, tasksThisMonth)
          }
          .collect { data ->
            if (data.pets.isEmpty()) {
              _uiState.value = HomeUiState(isLoading = false, isEmpty = true)
              return@collect
            }

            val petsWithSummary = data.pets.map { pet -> loadPetSummary(pet) }

            // Load timeline separately to avoid too many combine params
            val recentActivity = timelineRepository.getRecentActivity().first().take(10)
            val upcomingTimeline = timelineRepository.getUpcomingEvents(30).first().take(5)

            // Also include overdue tasks in the "today" section
            val overdueTasks = taskRepository.getPastDueTasks()
            val tasksDueToday =
              (overdueTasks + data.tasksDueToday).distinctBy { it.id }.sortedBy { it.scheduledFor }

            // Exclude today's tasks from week and week's from month
            val todayIds = tasksDueToday.map { it.id }.toSet()
            val tasksDueThisWeek = data.tasksDueThisWeek.filter { it.id !in todayIds }
            val weekIds = (todayIds + tasksDueThisWeek.map { it.id }).toSet()
            val tasksDueThisMonth = data.tasksDueThisMonth.filter { it.id !in weekIds }

            // Pre-compute merged task list (max 5) for the "Next Tasks" section
            val allTasks =
              (tasksDueToday + tasksDueThisWeek + tasksDueThisMonth)
                .distinctBy { it.id }
                .sortedBy { it.scheduledFor }
            val nextTasks = allTasks.take(HomeUiState.MAX_HOME_TASKS)
            val hasMoreTasks = allTasks.size > HomeUiState.MAX_HOME_TASKS

            _uiState.value =
              HomeUiState(
                isLoading = false,
                isRefreshing = false,
                pets = petsWithSummary,
                tasksDueToday = tasksDueToday,
                tasksDueThisWeek = tasksDueThisWeek,
                tasksDueThisMonth = tasksDueThisMonth,
                nextTasks = nextTasks,
                hasMoreTasks = hasMoreTasks,
                recentActivity = recentActivity,
                upcomingTimeline = upcomingTimeline,
                isEmpty = false,
              )
          }
      }
  }

  private data class DashboardData(
    val pets: List<Pet>,
    val tasksDueToday: List<Task>,
    val tasksDueThisWeek: List<Task>,
    val tasksDueThisMonth: List<Task>,
  )

  private suspend fun loadPetSummary(pet: Pet): PetWithSummary {
    val summary = getPetHealthSummary.execute(pet.id)

    return PetWithSummary(
      pet = pet,
      latestWeight = summary.latestWeight,
      weightStatus = summary.weightStatus,
      overallStatus = summary.overallStatus,
      nextVaccineType = summary.nextVaccineType,
      nextVaccinationDate = summary.nextVaccinationDate,
      nextDewormingType = summary.nextDewormingType,
      nextDewormingDate = summary.nextDewormingDate,
    )
  }

  fun refresh() {
    _uiState.value = _uiState.value.copy(isRefreshing = true)
    loadDashboard()
  }

  fun completeTask(taskId: String) {
    viewModelScope.launch {
      taskRepository.updateTaskStatus(
        taskId,
        com.woliveiras.petit.domain.model.TaskStatus.COMPLETED,
      )
    }
  }
}
