package com.woliveiras.petit.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskSchedulerInstrumentedTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val workManager = WorkManager.getInstance(context)
  private val scheduler = TaskSchedulerImpl(context)

  @Test
  fun replacesUniqueWorkAndCancelsTheTaskSchedule() {
    val taskId = "scheduler-${System.nanoTime()}"
    val task = task(taskId, LocalDateTime.now().plusDays(1))

    scheduler.scheduleTask(task)
    scheduler.scheduleTask(task.copy(scheduledFor = task.scheduledFor.plusDays(1)))

    val uniqueWorkName = "petit_task_$taskId"
    val active =
      workManager.getWorkInfosForUniqueWork(uniqueWorkName).get(5, TimeUnit.SECONDS).filter {
        it.state != WorkInfo.State.CANCELLED
      }
    assertThat(active).hasSize(1)
    assertThat(active.single().tags).contains("petit_task_$taskId")

    scheduler.cancelTask(taskId)

    var isCancelled = false
    repeat(20) {
      isCancelled =
        workManager.getWorkInfosForUniqueWork(uniqueWorkName).get(5, TimeUnit.SECONDS).all {
          it.state == WorkInfo.State.CANCELLED
        }
      if (!isCancelled) Thread.sleep(100)
    }
    assertThat(isCancelled).isTrue()
  }

  private fun task(id: String, scheduledFor: LocalDateTime) =
    Task(
      id = id,
      kind = TaskKind.CUSTOM,
      title = "Reminder",
      scheduledFor = scheduledFor,
      createdAt = 1L,
      updatedAt = 1L,
    )
}
