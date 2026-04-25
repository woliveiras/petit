package com.woliveiras.petit.presentation.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(val isCompleting: Boolean = false)

sealed interface OnboardingEvent {
  data object NavigateToHome : OnboardingEvent
}

@HiltViewModel
class OnboardingViewModel
@Inject
constructor(private val userPreferencesRepository: UserPreferencesRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(OnboardingUiState())
  val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<OnboardingEvent>()
  val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

  fun completeOnboarding() {
    viewModelScope.launch {
      _uiState.update { it.copy(isCompleting = true) }
      try {
        userPreferencesRepository.setOnboardingCompleted()
        _events.emit(OnboardingEvent.NavigateToHome)
      } finally {
        _uiState.update { it.copy(isCompleting = false) }
      }
    }
  }
}
