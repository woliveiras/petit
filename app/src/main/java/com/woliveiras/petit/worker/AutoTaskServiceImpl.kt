package com.woliveiras.petit.worker

import android.content.Context
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.ReminderPreferencesRepository
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.TaskStatus
import com.woliveiras.petit.domain.model.VaccinationEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [AutoTaskService] that creates automatic tasks for vaccination, deworming, and
 * weight tracking based on user preferences. Tasks are created when health records are saved.
 */
@Singleton
class AutoTaskServiceImpl
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val taskRepository: TaskRepository,
  private val reminderPreferencesRepository: ReminderPreferencesRepository,
  private val taskScheduler: TaskScheduler,
  private val petRepository: PetRepository,
) : AutoTaskService {

  override suspend fun handleVaccinationSaved(entry: VaccinationEntry) {
    val prefs = reminderPreferencesRepository.getPreferences()

    taskRepository.deleteTasksByReferenceEntity(entry.id)
    taskScheduler.cancelTask("auto_vacc_${entry.id}")

    if (!prefs.vaccinationRemindersEnabled || entry.nextDueDate == null) {
      return
    }

    val today = LocalDate.now()
    if (entry.nextDueDate.isBefore(today)) {
      return
    }

    val petName =
      petRepository.getPetById(entry.petId)?.name ?: context.getString(R.string.default_pet_name)

    val task =
      Task(
        id = "auto_vacc_${entry.id}",
        petId = entry.petId,
        kind = TaskKind.VACCINATION,
        referenceEntityId = entry.id,
        title = "$petName - ${entry.effectiveVaccineDisplayName}",
        description =
          context.getString(R.string.reminder_next_dose_description, prefs.vaccinationDaysBefore),
        scheduledFor =
          LocalDateTime.of(
            entry.nextDueDate,
            LocalTime.of(prefs.defaultNotificationHour, prefs.defaultNotificationMinute),
          ),
        status = TaskStatus.PENDING,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
      )

    taskRepository.saveTask(task)
    taskScheduler.scheduleTask(task)
  }

  override suspend fun handleVaccinationDeleted(entryId: String) {
    taskRepository.deleteTasksByReferenceEntity(entryId)
    taskScheduler.cancelTask("auto_vacc_$entryId")
  }

  override suspend fun handleDewormingSaved(entry: DewormingEntry) {
    val prefs = reminderPreferencesRepository.getPreferences()

    taskRepository.deleteTasksByReferenceEntity(entry.id)
    taskScheduler.cancelTask("auto_deworm_${entry.id}")

    if (!prefs.dewormingRemindersEnabled || entry.nextDueDate == null) {
      return
    }

    val today = LocalDate.now()
    if (entry.nextDueDate.isBefore(today)) {
      return
    }

    val petName =
      petRepository.getPetById(entry.petId)?.name ?: context.getString(R.string.default_pet_name)

    val task =
      Task(
        id = "auto_deworm_${entry.id}",
        petId = entry.petId,
        kind = TaskKind.DEWORMING,
        referenceEntityId = entry.id,
        title = "$petName - ${entry.type.displayName}",
        description =
          context.getString(R.string.reminder_next_dose_description, prefs.dewormingDaysBefore),
        scheduledFor =
          LocalDateTime.of(
            entry.nextDueDate,
            LocalTime.of(prefs.defaultNotificationHour, prefs.defaultNotificationMinute),
          ),
        status = TaskStatus.PENDING,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
      )

    taskRepository.saveTask(task)
    taskScheduler.scheduleTask(task)
  }

  override suspend fun handleDewormingDeleted(entryId: String) {
    taskRepository.deleteTasksByReferenceEntity(entryId)
    taskScheduler.cancelTask("auto_deworm_$entryId")
  }

  override suspend fun handleWeightSaved(petId: String, petName: String) {
    val prefs = reminderPreferencesRepository.getPreferences()

    if (!prefs.weightRemindersEnabled) {
      return
    }

    val taskId = "auto_weight_$petId"

    taskScheduler.cancelTask(taskId)
    taskRepository.deleteTask(taskId)

    val taskDate = LocalDate.now().plusDays(prefs.weightReminderIntervalDays.toLong())

    val task =
      Task(
        id = taskId,
        petId = petId,
        kind = TaskKind.WEIGHT,
        referenceEntityId = null,
        title = context.getString(R.string.reminder_weight_title, petName),
        description = context.getString(R.string.reminder_weight_description),
        scheduledFor =
          LocalDateTime.of(
            taskDate,
            LocalTime.of(prefs.defaultNotificationHour, prefs.defaultNotificationMinute),
          ),
        status = TaskStatus.PENDING,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
      )

    taskRepository.saveTask(task)
    taskScheduler.scheduleTask(task)
  }

  override suspend fun cancelWeightTask(petId: String) {
    val taskId = "auto_weight_$petId"
    taskScheduler.cancelTask(taskId)
    taskRepository.deleteTask(taskId)
  }
}
