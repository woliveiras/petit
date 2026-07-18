package com.woliveiras.petit.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.woliveiras.petit.BuildConfig
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.repository.DewormingEntryRepository
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.TaskRepository
import com.woliveiras.petit.data.repository.VaccinationEntryRepository
import com.woliveiras.petit.data.repository.WeightEntryRepository
import com.woliveiras.petit.domain.model.ConflictResolution
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.ImportAnalysis
import com.woliveiras.petit.domain.model.MergeResult
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.WeightEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/** Use case for exporting and importing app data as JSON. */
@Singleton
class ExportImportUseCase
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val database: PetitDatabase,
  private val petRepository: PetRepository,
  private val weightRepository: WeightEntryRepository,
  private val vaccinationRepository: VaccinationEntryRepository,
  private val dewormingRepository: DewormingEntryRepository,
  private val taskRepository: TaskRepository,
) {

  companion object {
    private const val MAX_IMPORT_SIZE_BYTES = 10L * 1024 * 1024 // 10MB
    private val ALLOWED_MIME_TYPES =
      listOf("application/json", "text/plain", "application/octet-stream")
  }

  /** Generate the filename for an export. */
  fun generateExportFilename(): String {
    val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    return "petit_backup_$date.json"
  }

  /** Export all data to a JSON bundle. */
  suspend fun exportAll(): ExportBundle =
    withContext(Dispatchers.IO) {
      val pets = petRepository.getAllPets().first()
      val weights = mutableListOf<WeightEntry>()
      val vaccinations = mutableListOf<VaccinationEntry>()
      val dewormings = mutableListOf<DewormingEntry>()
      val tasks = taskRepository.getAllActiveTasks().first()

      pets.forEach { pet ->
        weights.addAll(weightRepository.getWeightEntriesForPet(pet.id).first())
        vaccinations.addAll(vaccinationRepository.getVaccinationEntriesForPet(pet.id).first())
        dewormings.addAll(dewormingRepository.getDewormingEntriesForPet(pet.id).first())
      }

      ExportBundle(
        metadata =
          ExportMetadata(
            appVersion = BuildConfig.VERSION_NAME,
            exportDate = Instant.now().toString(),
            schemaVersion = 1,
          ),
        pets = pets,
        weightEntries = weights,
        vaccinationEntries = vaccinations,
        dewormingEntries = dewormings,
        tasks = tasks,
      )
    }

  /** Export data for a specific pet. */
  suspend fun exportForPet(petId: String): ExportBundle =
    withContext(Dispatchers.IO) {
      val pet = petRepository.getPetById(petId) ?: throw IllegalArgumentException("Pet not found")

      val weights = weightRepository.getWeightEntriesForPet(petId).first()
      val vaccinations = vaccinationRepository.getVaccinationEntriesForPet(petId).first()
      val dewormings = dewormingRepository.getDewormingEntriesForPet(petId).first()
      val tasks = taskRepository.getTasksForPet(petId).first()

      ExportBundle(
        metadata =
          ExportMetadata(
            appVersion = BuildConfig.VERSION_NAME,
            exportDate = Instant.now().toString(),
            schemaVersion = 1,
          ),
        pets = listOf(pet),
        weightEntries = weights,
        vaccinationEntries = vaccinations,
        dewormingEntries = dewormings,
        tasks = tasks,
      )
    }

  /** Write an export bundle to a URI. */
  suspend fun writeExportToUri(bundle: ExportBundle, uri: Uri) =
    withContext(Dispatchers.IO) {
      val json = bundle.toJson().toString(2)
      context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(json.toByteArray(Charsets.UTF_8))
      } ?: throw IllegalStateException("Cannot open output stream")
    }

  /** Read and parse a JSON file from a URI. */
  suspend fun readImportFromUri(uri: Uri): ExportBundle =
    withContext(Dispatchers.IO) {
      // Validate MIME type
      val mimeType = context.contentResolver.getType(uri)
      if (mimeType != null && mimeType !in ALLOWED_MIME_TYPES) {
        throw IllegalArgumentException("Tipo de arquivo inválido: $mimeType")
      }

      // Validate file size
      context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
        val fileSize = fd.statSize
        if (fileSize > MAX_IMPORT_SIZE_BYTES) {
          throw IllegalArgumentException(
            "Arquivo muito grande: máximo ${MAX_IMPORT_SIZE_BYTES / 1024 / 1024}MB"
          )
        }
      }

      val json =
        context.contentResolver.openInputStream(uri)?.use { stream ->
          stream.bufferedReader().readText()
        } ?: throw IllegalStateException("Cannot read file")

      try {
        val bundle = ExportBundle.fromJson(JSONObject(json))
        val errors = ExportBundle.validate(bundle)
        if (errors.isNotEmpty()) {
          throw IllegalArgumentException("Dados inválidos: ${errors.first()}")
        }
        bundle
      } catch (e: JSONException) {
        throw IllegalArgumentException("Invalid JSON format: ${e.message}")
      } catch (e: IllegalArgumentException) {
        throw e
      } catch (e: Exception) {
        throw IllegalArgumentException("Error parsing file: ${e.message}")
      }
    }

  /** Analyze an import bundle for conflicts. */
  suspend fun analyzeImport(bundle: ExportBundle): ImportAnalysis =
    withContext(Dispatchers.IO) {
      val existingPets = petRepository.getAllPets().first()
      val existingPetIds = existingPets.map { it.id }.toSet()
      val existingPetNames = existingPets.associate { it.id to it.name }

      val conflictingPetNames =
        bundle.pets.filter { it.id in existingPetIds }.map { existingPetNames[it.id] ?: it.name }

      ImportAnalysis(
        totalPets = bundle.pets.size,
        totalWeightEntries = bundle.weightEntries.size,
        totalVaccinationEntries = bundle.vaccinationEntries.size,
        totalDewormingEntries = bundle.dewormingEntries.size,
        totalTasks = bundle.tasks.size,
        conflictingPetNames = conflictingPetNames,
        schemaVersion = bundle.metadata.schemaVersion,
        exportDate = bundle.metadata.exportDate,
      )
    }

  /** Import data from a bundle with the specified conflict resolution strategy. */
  suspend fun importData(
    bundle: ExportBundle,
    conflictResolution: ConflictResolution,
  ): MergeResult =
    withContext(Dispatchers.IO) {
      val validationErrors = ExportBundle.validate(bundle)
      require(validationErrors.isEmpty()) { "Dados inválidos: ${validationErrors.first()}" }

      if (conflictResolution == ConflictResolution.REPLACE) {
        return@withContext replaceShareableData(bundle)
      }

      database.withTransaction {
        var petsAdded = 0
        var petsUpdated = 0
        var weightsAdded = 0
        var weightsUpdated = 0
        var vaccinationsAdded = 0
        var vaccinationsUpdated = 0
        var dewormingsAdded = 0
        var dewormingsUpdated = 0
        var tasksAdded = 0
        var tasksUpdated = 0

        val existingPetIds = petRepository.getAllPets().first().map { it.id }.toSet()

        // Import pets
        for (pet in bundle.pets) {
          val exists = pet.id in existingPetIds
          when {
            !exists -> {
              petRepository.savePet(pet)
              petsAdded++
            }
            conflictResolution == ConflictResolution.REPLACE -> {
              petRepository.savePet(pet)
              petsUpdated++
            }
            conflictResolution == ConflictResolution.MERGE -> {
              val existing = petRepository.getPetById(pet.id)
              if (existing == null || pet.updatedAt > existing.updatedAt) {
                petRepository.savePet(pet)
                petsUpdated++
              }
            }
          // KEEP: do nothing for existing pets
          }
        }

        // Import weight entries
        for (entry in bundle.weightEntries) {
          val existing = weightRepository.getWeightEntryById(entry.id)
          when {
            existing == null -> {
              weightRepository.saveWeightEntry(entry)
              weightsAdded++
            }
            conflictResolution == ConflictResolution.REPLACE -> {
              weightRepository.saveWeightEntry(entry)
              weightsUpdated++
            }
            conflictResolution == ConflictResolution.MERGE &&
              entry.updatedAt > existing.updatedAt -> {
              weightRepository.saveWeightEntry(entry)
              weightsUpdated++
            }
          }
        }

        // Import vaccination entries
        for (entry in bundle.vaccinationEntries) {
          val existing = vaccinationRepository.getVaccinationEntryById(entry.id)
          when {
            existing == null -> {
              vaccinationRepository.saveVaccinationEntry(entry)
              vaccinationsAdded++
            }
            conflictResolution == ConflictResolution.REPLACE -> {
              vaccinationRepository.saveVaccinationEntry(entry)
              vaccinationsUpdated++
            }
            conflictResolution == ConflictResolution.MERGE &&
              entry.updatedAt > existing.updatedAt -> {
              vaccinationRepository.saveVaccinationEntry(entry)
              vaccinationsUpdated++
            }
          }
        }

        // Import deworming entries
        for (entry in bundle.dewormingEntries) {
          val existing = dewormingRepository.getDewormingEntryById(entry.id)
          when {
            existing == null -> {
              dewormingRepository.saveDewormingEntry(entry)
              dewormingsAdded++
            }
            conflictResolution == ConflictResolution.REPLACE -> {
              dewormingRepository.saveDewormingEntry(entry)
              dewormingsUpdated++
            }
            conflictResolution == ConflictResolution.MERGE &&
              entry.updatedAt > existing.updatedAt -> {
              dewormingRepository.saveDewormingEntry(entry)
              dewormingsUpdated++
            }
          }
        }

        // Import tasks
        for (entry in bundle.tasks) {
          val existing = taskRepository.getTaskById(entry.id)
          when {
            existing == null -> {
              taskRepository.saveTask(entry)
              tasksAdded++
            }
            conflictResolution == ConflictResolution.REPLACE -> {
              taskRepository.saveTask(entry)
              tasksUpdated++
            }
            conflictResolution == ConflictResolution.MERGE &&
              entry.updatedAt > existing.updatedAt -> {
              taskRepository.saveTask(entry)
              tasksUpdated++
            }
          }
        }

        MergeResult(
          petsAdded = petsAdded,
          petsUpdated = petsUpdated,
          weightsAdded = weightsAdded,
          weightsUpdated = weightsUpdated,
          vaccinationsAdded = vaccinationsAdded,
          vaccinationsUpdated = vaccinationsUpdated,
          dewormingsAdded = dewormingsAdded,
          dewormingsUpdated = dewormingsUpdated,
          tasksAdded = tasksAdded,
          tasksUpdated = tasksUpdated,
          conflictsResolved =
            petsUpdated + weightsUpdated + vaccinationsUpdated + dewormingsUpdated + tasksUpdated,
        )
      }
    }

  private suspend fun replaceShareableData(bundle: ExportBundle): MergeResult =
    database.withTransaction {
      val existingPets = petRepository.getAllPets().first()
      val existingWeights =
        existingPets.flatMap { weightRepository.getWeightEntriesForPet(it.id).first() }
      val existingVaccinations =
        existingPets.flatMap { vaccinationRepository.getVaccinationEntriesForPet(it.id).first() }
      val existingDewormings =
        existingPets.flatMap { dewormingRepository.getDewormingEntriesForPet(it.id).first() }
      val existingTasks = taskRepository.getAllActiveTasks().first()

      database.taskDao().deleteAll()
      database.dewormingEntryDao().deleteAll()
      database.vaccinationEntryDao().deleteAll()
      database.weightEntryDao().deleteAll()
      database.petDao().deleteAll()

      bundle.pets.forEach { petRepository.savePet(it) }
      bundle.weightEntries.forEach { weightRepository.saveWeightEntry(it) }
      bundle.vaccinationEntries.forEach { vaccinationRepository.saveVaccinationEntry(it) }
      bundle.dewormingEntries.forEach { dewormingRepository.saveDewormingEntry(it) }
      bundle.tasks.forEach { taskRepository.saveTask(it) }

      val petCounts = replacementCounts(existingPets.map { it.id }, bundle.pets.map { it.id })
      val weightCounts =
        replacementCounts(existingWeights.map { it.id }, bundle.weightEntries.map { it.id })
      val vaccinationCounts =
        replacementCounts(
          existingVaccinations.map { it.id },
          bundle.vaccinationEntries.map { it.id },
        )
      val dewormingCounts =
        replacementCounts(existingDewormings.map { it.id }, bundle.dewormingEntries.map { it.id })
      val taskCounts = replacementCounts(existingTasks.map { it.id }, bundle.tasks.map { it.id })

      MergeResult(
        petsAdded = petCounts.added,
        petsUpdated = petCounts.updated,
        petsRemoved = petCounts.removed,
        weightsAdded = weightCounts.added,
        weightsUpdated = weightCounts.updated,
        weightsRemoved = weightCounts.removed,
        vaccinationsAdded = vaccinationCounts.added,
        vaccinationsUpdated = vaccinationCounts.updated,
        vaccinationsRemoved = vaccinationCounts.removed,
        dewormingsAdded = dewormingCounts.added,
        dewormingsUpdated = dewormingCounts.updated,
        dewormingsRemoved = dewormingCounts.removed,
        tasksAdded = taskCounts.added,
        tasksUpdated = taskCounts.updated,
        tasksRemoved = taskCounts.removed,
        conflictsResolved =
          petCounts.updated +
            weightCounts.updated +
            vaccinationCounts.updated +
            dewormingCounts.updated +
            taskCounts.updated,
      )
    }

  private data class ReplacementCounts(val added: Int, val updated: Int, val removed: Int)

  private fun replacementCounts(existingIds: List<String>, incomingIds: List<String>) =
    ReplacementCounts(
      added = (incomingIds.toSet() - existingIds.toSet()).size,
      updated = (incomingIds.toSet() intersect existingIds.toSet()).size,
      removed = (existingIds.toSet() - incomingIds.toSet()).size,
    )
}
