package com.woliveiras.petit.presentation.feature.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.domain.model.Pet
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PetSelectionUiState(val pets: List<Pet> = emptyList(), val isLoading: Boolean = true)

@HiltViewModel
class PetSelectionViewModel @Inject constructor(petRepository: PetRepository) : ViewModel() {

  val uiState: StateFlow<PetSelectionUiState> =
    petRepository
      .getAllPets()
      .map { pets -> PetSelectionUiState(pets = pets, isLoading = false) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PetSelectionUiState(),
      )
}
