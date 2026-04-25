package com.woliveiras.petit.presentation.feature.deworming

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.DewormingEntryRepository
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.SyncStatus
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

/** UI state for deworming screens. */
data class DewormingUiState(
  val petId: String = "",
  val petName: String = "",
  val isLoading: Boolean = true,
  val latestDewormings: List<DewormingEntry> = emptyList(),
  val allDewormings: List<DewormingEntry> = emptyList(),
  val form: DewormingFormState = DewormingFormState(),
) {
  /** Dewormings grouped by type, showing only the latest of each. */
  val dewormingsByType: Map<DewormingType, DewormingEntry>
    get() = latestDewormings.associateBy { it.type }
}

/** Form-specific state for deworming entry creation/editing. */
data class DewormingFormState(
  val isEditMode: Boolean = false,
  val editingEntryId: String? = null,
  val dewormingType: DewormingType = DewormingType.INTERNAL,
  val medication: String = "",
  val applicationDate: LocalDate = LocalDate.now(),
  val nextDueDate: LocalDate? = null,
  val note: String = "",
  val isSaving: Boolean = false,
  val medicationError: String? = null,
  val applicationDateError: String? = null,
  val noteError: String? = null,
)

/** Events emitted by deworming ViewModel. */
sealed class DewormingEvent {
  data class DewormingSaved(val petId: String) : DewormingEvent()

  data class DewormingDeleted(val petId: String) : DewormingEvent()

  data class Error(val message: String) : DewormingEvent()
}

@HiltViewModel
class DewormingViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val petRepository: PetRepository,
  private val dewormingRepository: DewormingEntryRepository,
  private val autoTaskService: AutoTaskService,
) : ViewModel() {

  private val petId: String = savedStateHandle.get<String>("petId") ?: ""

  private val _uiState = MutableStateFlow(DewormingUiState(petId = petId))
  val uiState: StateFlow<DewormingUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<DewormingEvent>()
  val events: SharedFlow<DewormingEvent> = _events.asSharedFlow()

  init {
    loadPetInfo()
    loadDewormings()
  }

  private fun loadPetInfo() {
    viewModelScope.launch {
      petRepository.getPetById(petId)?.let { pet ->
        _uiState.update { it.copy(petName = pet.name) }
      }
    }
  }

  private fun loadDewormings() {
    viewModelScope.launch {
      dewormingRepository.getLatestDewormingsForPet(petId).collect { latest ->
        _uiState.update { it.copy(latestDewormings = latest, isLoading = false) }
      }
    }
    viewModelScope.launch {
      dewormingRepository.getDewormingEntriesForPet(petId).collect { all ->
        _uiState.update { it.copy(allDewormings = all) }
      }
    }
  }

  // ===== Form methods =====

  fun updateDewormingType(type: DewormingType) {
    _uiState.update { state ->
      val form = state.form
      // Auto-suggest next dose date based on deworming interval
      val suggestedNextDue =
        if (!form.isEditMode && form.nextDueDate == null) {
          form.applicationDate.plusMonths(type.defaultIntervalMonths.toLong())
        } else {
          form.nextDueDate
        }
      state.copy(form = form.copy(dewormingType = type, nextDueDate = suggestedNextDue))
    }
  }

  fun updateMedication(medication: String) {
    _uiState.update {
      it.copy(form = it.form.copy(medication = medication, medicationError = null))
    }
  }

  fun updateApplicationDate(date: LocalDate) {
    _uiState.update { state ->
      val form = state.form
      // Auto-update next dose date based on deworming interval
      val suggestedNextDue = date.plusMonths(form.dewormingType.defaultIntervalMonths.toLong())
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

  fun updateNote(value: String) {
    _uiState.update { it.copy(form = it.form.copy(note = value, noteError = null)) }
  }

  /** Load an existing entry for editing. */
  fun loadEntryForEdit(entryId: String) {
    viewModelScope.launch {
      val entry = dewormingRepository.getDewormingEntryById(entryId)
      if (entry != null) {
        _uiState.update {
          it.copy(
            form =
              it.form.copy(
                isEditMode = true,
                editingEntryId = entry.id,
                dewormingType = entry.type,
                medication = entry.medication ?: "",
                applicationDate = entry.applicationDate,
                nextDueDate = entry.nextDueDate,
                note = entry.note ?: "",
              )
          )
        }
      }
    }
  }

  fun resetForm() {
    _uiState.update { it.copy(form = DewormingFormState()) }
  }

  fun saveDeworming() {
    val state = _uiState.value
    val form = state.form

    // Validation
    if (form.medication.isBlank()) {
      _uiState.update {
        it.copy(
          form =
            it.form.copy(
              medicationError = context.getString(R.string.deworming_error_medication_required)
            )
        )
      }
      return
    }

    if (form.medication.length > 100) {
      _uiState.update {
        it.copy(
          form =
            it.form.copy(
              medicationError = context.getString(R.string.deworming_error_medication_max_length)
            )
        )
      }
      return
    }

    if (form.applicationDate.isAfter(LocalDate.now())) {
      _uiState.update {
        it.copy(
          form =
            it.form.copy(
              applicationDateError = context.getString(R.string.deworming_error_date_future)
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
              applicationDateError = context.getString(R.string.deworming_error_next_dose_after)
            )
        )
      }
      return
    }

    if (form.note.length > 500) {
      _uiState.update {
        it.copy(
          form =
            it.form.copy(noteError = context.getString(R.string.deworming_error_note_max_length))
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
            state.allDewormings.find { it.id == form.editingEntryId }
          } else null

        val entry =
          DewormingEntry(
            id = form.editingEntryId ?: UUID.randomUUID().toString(),
            petId = petId,
            type = form.dewormingType,
            medication = form.medication.trim(),
            applicationDate = form.applicationDate,
            nextDueDate = form.nextDueDate,
            note = form.note.trim().ifBlank { null },
            createdAt = existingEntry?.createdAt ?: now,
            updatedAt = now,
            syncStatus = SyncStatus.LOCAL_ONLY,
          )

        dewormingRepository.saveDewormingEntry(entry)

        // Create/update automatic task if nextDueDate is set
        try {
          autoTaskService.handleDewormingSaved(entry)
        } catch (_: Exception) {
          // DB saved but auto-task failed — non-critical
        }

        _uiState.update { it.copy(form = DewormingFormState()) }

        _events.emit(DewormingEvent.DewormingSaved(petId))
      } catch (e: Exception) {
        _events.emit(
          DewormingEvent.Error(e.message ?: context.getString(R.string.deworming_error_save))
        )
      } finally {
        _uiState.update { it.copy(form = it.form.copy(isSaving = false)) }
      }
    }
  }

  fun deleteEntry(entryId: String) {
    viewModelScope.launch {
      try {
        dewormingRepository.deleteDewormingEntry(entryId)
        autoTaskService.handleDewormingDeleted(entryId)
      } catch (e: Exception) {
        _events.emit(
          DewormingEvent.Error(e.message ?: context.getString(R.string.deworming_error_delete))
        )
      }
    }
  }

  /** Delete the currently editing entry and navigate back. */
  fun deleteCurrentEntry() {
    val entryId = _uiState.value.form.editingEntryId ?: return
    viewModelScope.launch {
      try {
        dewormingRepository.deleteDewormingEntry(entryId)
        autoTaskService.handleDewormingDeleted(entryId)
        _events.emit(DewormingEvent.DewormingDeleted(petId))
      } catch (e: Exception) {
        _events.emit(
          DewormingEvent.Error(e.message ?: context.getString(R.string.deworming_error_delete))
        )
      }
    }
  }
}
