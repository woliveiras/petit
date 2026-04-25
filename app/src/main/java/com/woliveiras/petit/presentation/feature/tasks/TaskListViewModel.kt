package com.woliveiras.petit.presentation.feature.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.worker.TaskScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for task list screen. */
data class TaskListUiState(
  val isLoading: Boolean = true,
  val tasks: List<Task> = emptyList(),
  val activeFilter: TaskFilter = TaskFilter.THIS_WEEK,
)

/** Events emitted by TaskListViewModel. */
sealed class TaskListEvent {
  data class NavigateToRecord(val kind: TaskKind, val petId: String?, val entryId: String?) :
    TaskListEvent()

  data class NavigateToTaskForm(val taskId: String?) : TaskListEvent()

  data class Error(val message: String) : TaskListEvent()
}

@HiltViewModel
class TaskListViewModel
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val taskRepository: TaskRepository,
  private val taskScheduler: TaskScheduler,
) : ViewModel() {

  private val _uiState = MutableStateFlow(TaskListUiState())
  val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<TaskListEvent>()
  val events: SharedFlow<TaskListEvent> = _events.asSharedFlow()

  init {
    loadTasks()
  }

  fun setFilter(filter: TaskFilter) {
    _uiState.update { it.copy(activeFilter = filter) }
    loadTasks()
  }

  private fun loadTasks() {
    viewModelScope.launch {
      val filter = _uiState.value.activeFilter
      val (from, to) = getTimeRange(filter)
      taskRepository.getTasksDueInRange(from, to).collect { tasks ->
        // Also include overdue tasks (past due, still pending)
        val overdueTasks = taskRepository.getPastDueTasks()
        val allTasks = (overdueTasks + tasks).distinctBy { it.id }.sortedBy { it.scheduledFor }
        _uiState.update { it.copy(isLoading = false, tasks = allTasks) }
      }
    }
  }

  fun completeTask(taskId: String) {
    viewModelScope.launch {
      try {
        taskScheduler.cancelTask(taskId)
        taskRepository.updateTaskStatus(taskId, TaskStatus.COMPLETED)
      } catch (e: Exception) {
        _events.emit(
          TaskListEvent.Error(e.message ?: context.getString(R.string.task_error_complete))
        )
      }
    }
  }

  fun deleteTask(taskId: String) {
    viewModelScope.launch {
      try {
        taskScheduler.cancelTask(taskId)
        taskRepository.deleteTask(taskId)
      } catch (e: Exception) {
        _events.emit(
          TaskListEvent.Error(e.message ?: context.getString(R.string.task_error_delete))
        )
      }
    }
  }

  fun onTaskClicked(task: Task) {
    viewModelScope.launch {
      if (task.referenceEntityId != null && task.petId != null) {
        _events.emit(TaskListEvent.NavigateToRecord(task.kind, task.petId, task.referenceEntityId))
      } else {
        _events.emit(TaskListEvent.NavigateToTaskForm(task.id))
      }
    }
  }

  private fun getTimeRange(filter: TaskFilter): Pair<Long, Long> {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()

    return when (filter) {
      TaskFilter.TODAY -> {
        val start = today.atStartOfDay().atZone(zone).toInstant().toEpochMilli()
        val end = today.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
        start to end
      }
      TaskFilter.THIS_WEEK -> {
        val start = today.atStartOfDay().atZone(zone).toInstant().toEpochMilli()
        val end = today.plusDays(6).atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
        start to end
      }
      TaskFilter.THIS_MONTH -> {
        val yearMonth = YearMonth.from(today)
        val start = yearMonth.atDay(1).atStartOfDay().atZone(zone).toInstant().toEpochMilli()
        val end =
          yearMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
        start to end
      }
      TaskFilter.NEXT_90_DAYS -> {
        val start = today.atStartOfDay().atZone(zone).toInstant().toEpochMilli()
        val end = today.plusDays(90).atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
        start to end
      }
      TaskFilter.THIS_YEAR -> {
        val start = today.withDayOfYear(1).atStartOfDay().atZone(zone).toInstant().toEpochMilli()
        val end =
          today
            .withMonth(12)
            .withDayOfMonth(31)
            .atTime(LocalTime.MAX)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        start to end
      }
    }
  }
}
