package com.woliveiras.petit.presentation.feature.vaccination

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.VaccinationEntryRepository
import com.woliveiras.petit.domain.model.PetType
import com.woliveiras.petit.domain.model.SyncStatus
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.worker.AutoTaskService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for vaccination screens. */
data class VaccinationUiState(
  val petId: String = "",
  val petName: String = "",
  val petType: PetType = PetType.OTHER,
  val isLoading: Boolean = true,
  val latestVaccinations: List<VaccinationEntry> = emptyList(),
  val allVaccinations: List<VaccinationEntry> = emptyList(),
  val form: VaccinationFormState = VaccinationFormState(),
) {
  /** Vaccinations grouped by type, showing only the latest of each. */
  val vaccinationsByType: Map<VaccineType, VaccinationEntry>
    get() = latestVaccinations.associateBy { it.vaccineType }

  /** Vaccine types available for this pet's species. */
  val availableVaccineTypes: List<VaccineType>
    get() = VaccineType.forPetType(petType)
}

/** Form-specific state for vaccination entry creation/editing. */
data class VaccinationFormState(
  val isEditMode: Boolean = false,
  val editingEntryId: String? = null,
  val vaccineType: VaccineType = VaccineType.V3,
  val customName: String = "",
  val applicationDate: LocalDate = LocalDate.now(),
  val nextDueDate: LocalDate? = null,
  val veterinarian: String = "",
  val clinic: String = "",
  val batchNumber: String = "",
  val note: String = "",
  val isSaving: Boolean = false,
  val vaccineTypeError: String? = null,
  val applicationDateError: String? = null,
  val veterinarianError: String? = null,
  val clinicError: String? = null,
  val batchNumberError: String? = null,
  val noteError: String? = null,
  val customNameError: String? = null,
)

/** Events emitted by vaccination ViewModel. */
sealed class VaccinationEvent {
  data class VaccinationSaved(val petId: String) : VaccinationEvent()

  data class VaccinationDeleted(val petId: String) : VaccinationEvent()

  data class Error(val message: String) : VaccinationEvent()
}

@HiltViewModel
class VaccinationViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val petRepository: PetRepository,
  private val vaccinationRepository: VaccinationEntryRepository,
  private val autoTaskService: AutoTaskService,
) : ViewModel() {

  private val petId: String = savedStateHandle.get<String>("petId") ?: ""

  private val _uiState = MutableStateFlow(VaccinationUiState(petId = petId))
  val uiState: StateFlow<VaccinationUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<VaccinationEvent>()
  val events: SharedFlow<VaccinationEvent> = _events.asSharedFlow()

  init {
    loadPetInfo()
    loadVaccinations()
  }

  private fun loadPetInfo() {
    viewModelScope.launch {
      petRepository.getPetById(petId)?.let { pet ->
        _uiState.update { it.copy(petName = pet.name, petType = pet.petType) }
      }
    }
  }

  private fun loadVaccinations() {
    viewModelScope.launch {
      vaccinationRepository.getLatestVaccinationsForPet(petId).collect { latest ->
        _uiState.update { it.copy(latestVaccinations = latest, isLoading = false) }
      }
    }
    viewModelScope.launch {
      vaccinationRepository.getVaccinationEntriesForPet(petId).collect { all ->
        _uiState.update { it.copy(allVaccinations = all) }
      }
    }
  }

  // ===== Form methods =====

  fun updateVaccineType(type: VaccineType) {
    _uiState.update { state ->
      val form = state.form
      // Auto-suggest next dose date based on vaccine interval
      val suggestedNextDue =
        if (!form.isEditMode && form.nextDueDate == null) {
          type.defaultIntervalMonths?.let { months ->
            form.applicationDate.plusMonths(months.toLong())
          }
        } else {
          form.nextDueDate
        }
      state.copy(
        form =
          form.copy(vaccineType = type, vaccineTypeError = null, nextDueDate = suggestedNextDue)
      )
    }
  }

  fun updateCustomName(name: String) {
    _uiState.update { it.copy(form = it.form.copy(customName = name)) }
  }

  fun updateApplicationDate(date: LocalDate) {
    _uiState.update { state ->
      val form = state.form
      // Auto-update next dose date if vaccine has default interval
      val suggestedNextDue =
        form.vaccineType.defaultIntervalMonths?.let { months -> date.plusMonths(months.toLong()) }
      state.copy(
        form =
          form.copy(
            applicationDate = date,
            applicationDateError = null,
            nextDueDate = suggestedNextDue,
          )
      )
    }
  }

  fun updateNextDueDate(date: LocalDate?) {
    _uiState.update { it.copy(form = it.form.copy(nextDueDate = date)) }
  }

  fun updateVeterinarian(value: String) {
    _uiState.update { it.copy(form = it.form.copy(veterinarian = value, veterinarianError = null)) }
  }

  fun updateClinic(value: String) {
    _uiState.update { it.copy(form = it.form.copy(clinic = value, clinicError = null)) }
  }

  fun updateBatchNumber(value: String) {
    _uiState.update { it.copy(form = it.form.copy(batchNumber = value, batchNumberError = null)) }
  }

  fun updateNote(value: String) {
    _uiState.update { it.copy(form = it.form.copy(note = value, noteError = null)) }
  }

  /** Load an existing entry for editing. */
  fun loadEntryForEdit(entryId: String) {
    viewModelScope.launch {
      val entry = vaccinationRepository.getVaccinationEntryById(entryId)
      if (entry != null) {
        _uiState.update {
          it.copy(
            form =
              it.form.copy(
                isEditMode = true,
                editingEntryId = entry.id,
                vaccineType = entry.vaccineType,
                customName = entry.customVaccineTypeName ?: "",
                applicationDate = entry.applicationDate,
                nextDueDate = entry.nextDueDate,
                veterinarian = entry.veterinarian ?: "",
                clinic = entry.clinic ?: "",
                batchNumber = entry.batchNumber ?: "",
                note = entry.note ?: "",
              )
          )
        }
      }
    }
  }

  fun resetForm() {
    _uiState.update { it.copy(form = VaccinationFormState()) }
  }

  fun saveVaccination() {
    val state = _uiState.value
    val form = state.form

    // Validation
    if (form.applicationDate.isAfter(LocalDate.now())) {
      _uiState.update {
        it.copy(
          form =
            it.form.copy(
              applicationDateError = context.getString(R.string.vaccination_error_date_future)
            )
        )
      }
      return
    }

    if (form.nextDueDate != null && !form.nextDueDate.isAfter(form.applicationDate)) {
      _uiState.update {
        it.copy(
          form =
            it.form.copy(
              applicationDateError = context.getString(R.string.vaccination_error_next_dose_after)
            )
        )
      }
      return
    }

    if (form.veterinarian.length > 100) {
      _uiState.update {
        it.copy(
          form =
            it.form.copy(
              veterinarianError =
                context.getString(R.string.vaccination_error_field_max_length, 100)
            )
        )
      }
      return
    }

    if (form.clinic.length > 100) {
      _uiState.update {
        it.copy(
          form =
            it.form.copy(
              clinicError = context.getString(R.string.vaccination_error_field_max_length, 100)
            )
        )
      }
      return
    }

    if (form.batchNumber.length > 50) {
      _uiState.update {
        it.copy(
          form =
            it.form.copy(
              batchNumberError = context.getString(R.string.vaccination_error_field_max_length, 50)
            )
        )
      }
      return
    }

    if (form.note.length > 500) {
      _uiState.update {
        it.copy(
          form =
            it.form.copy(noteError = context.getString(R.string.vaccination_error_note_max_length))
        )
      }
      return
    }

    if (form.vaccineType == VaccineType.OTHER && form.customName.trim().length > 100) {
      _uiState.update {
        it.copy(
          form =
            it.form.copy(
              customNameError = context.getString(R.string.vaccination_error_field_max_length, 100)
            )
        )
      }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(form = it.form.copy(isSaving = true)) }

      try {
        val now = System.currentTimeMillis()
        val existingEntry =
          if (form.isEditMode) {
            state.allVaccinations.find { it.id == form.editingEntryId }
          } else null

        val entry =
          VaccinationEntry(
            id = form.editingEntryId ?: UUID.randomUUID().toString(),
            petId = petId,
            vaccineType = form.vaccineType,
            customVaccineTypeName =
              if (form.vaccineType == VaccineType.OTHER) form.customName.trim().ifBlank { null }
              else null,
            applicationDate = form.applicationDate,
            nextDueDate = form.nextDueDate,
            veterinarian = form.veterinarian.trim().ifBlank { null },
            clinic = form.clinic.trim().ifBlank { null },
            batchNumber = form.batchNumber.trim().ifBlank { null },
            note = form.note.trim().ifBlank { null },
            createdAt = existingEntry?.createdAt ?: now,
            updatedAt = now,
            syncStatus = SyncStatus.LOCAL_ONLY,
          )

        vaccinationRepository.saveVaccinationEntry(entry)

        // Create/update automatic task if nextDueDate is set
        try {
          autoTaskService.handleVaccinationSaved(entry)
        } catch (_: Exception) {
          // DB saved but auto-task failed — non-critical
        }

        _uiState.update { it.copy(form = VaccinationFormState()) }

        _events.emit(VaccinationEvent.VaccinationSaved(petId))
      } catch (e: Exception) {
        _events.emit(
          VaccinationEvent.Error(e.message ?: context.getString(R.string.vaccination_error_save))
        )
      } finally {
        _uiState.update { it.copy(form = it.form.copy(isSaving = false)) }
      }
    }
  }

  fun deleteEntry(entryId: String) {
    viewModelScope.launch {
      try {
        vaccinationRepository.deleteVaccinationEntry(entryId)
        autoTaskService.handleVaccinationDeleted(entryId)
      } catch (e: Exception) {
        _events.emit(
          VaccinationEvent.Error(e.message ?: context.getString(R.string.vaccination_error_delete))
        )
      }
    }
  }

  /** Delete the currently editing entry and navigate back. */
  fun deleteCurrentEntry() {
    val entryId = _uiState.value.form.editingEntryId ?: return
    viewModelScope.launch {
      try {
        vaccinationRepository.deleteVaccinationEntry(entryId)
        autoTaskService.handleVaccinationDeleted(entryId)
        _events.emit(VaccinationEvent.VaccinationDeleted(petId))
      } catch (e: Exception) {
        _events.emit(
          VaccinationEvent.Error(e.message ?: context.getString(R.string.vaccination_error_delete))
        )
      }
    }
  }
}
