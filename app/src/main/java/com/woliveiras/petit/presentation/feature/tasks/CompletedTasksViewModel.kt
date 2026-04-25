package com.woliveiras.petit.presentation.feature.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.worker.TaskScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for completed tasks screen. */
data class CompletedTasksUiState(val isLoading: Boolean = true, val tasks: List<Task> = emptyList())

/** Events emitted by CompletedTasksViewModel. */
sealed class CompletedTasksEvent {
  data class Error(val message: String) : CompletedTasksEvent()
}

@HiltViewModel
class CompletedTasksViewModel
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val taskRepository: TaskRepository,
  private val taskScheduler: TaskScheduler,
) : ViewModel() {

  private val _uiState = MutableStateFlow(CompletedTasksUiState())
  val uiState: StateFlow<CompletedTasksUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<CompletedTasksEvent>()
  val events: SharedFlow<CompletedTasksEvent> = _events.asSharedFlow()

  init {
    loadCompletedTasks()
  }

  private fun loadCompletedTasks() {
    viewModelScope.launch {
      taskRepository.getCompletedTasks().collect { tasks ->
        _uiState.update { it.copy(isLoading = false, tasks = tasks) }
      }
    }
  }

  fun reactivateTask(taskId: String) {
    viewModelScope.launch {
      try {
        taskRepository.updateTaskStatus(taskId, TaskStatus.PENDING)
        // Reschedule notification
        val task = taskRepository.getTaskById(taskId)
        if (task != null) {
          taskScheduler.scheduleTask(task)
        }
      } catch (e: Exception) {
        _events.emit(
          CompletedTasksEvent.Error(e.message ?: context.getString(R.string.task_error_reactivate))
        )
      }
    }
  }

  fun deleteTask(taskId: String) {
    viewModelScope.launch {
      try {
        taskRepository.deleteTask(taskId)
      } catch (e: Exception) {
        _events.emit(
          CompletedTasksEvent.Error(e.message ?: context.getString(R.string.task_error_delete))
        )
      }
    }
  }
}
