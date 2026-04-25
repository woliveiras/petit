package com.woliveiras.petit.presentation.feature.settings

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.domain.model.FamilyGroupInfo
import com.woliveiras.petit.domain.usecase.DeleteAllDataAction
import com.woliveiras.petit.util.LocaleHelper
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

data class SettingsUiState(
  val currentTheme: AppTheme = AppTheme.SYSTEM,
  val currentLanguage: AppLanguage = AppLanguage.SYSTEM,
  val availableThemes: List<AppTheme> = AppTheme.entries,
  val availableLanguages: List<AppLanguage> = AppLanguage.entries,
  val showThemeDialog: Boolean = false,
  val showLanguageDialog: Boolean = false,
  val showDeleteAllDataDialog: Boolean = false,
  val isDeletingAllData: Boolean = false,
  val familyGroupInfo: FamilyGroupInfo? = null,
  val lastSyncText: String? = null,
)

sealed class SettingsEvent {
  data object DeleteAllDataSuccess : SettingsEvent()

  data class DeleteAllDataError(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val familyGroupRepository: FamilyGroupRepository,
  private val deleteAllDataUseCase: DeleteAllDataAction,
) : ViewModel() {

  private val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<SettingsEvent>()
  val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

  init {
    observePreferences()
    observeFamilyGroup()
  }

  private fun observePreferences() {
    viewModelScope.launch {
      userPreferencesRepository.userPreferences.collect { prefs ->
        _uiState.update { it.copy(currentTheme = prefs.theme, currentLanguage = prefs.language) }
      }
    }
  }

  private fun observeFamilyGroup() {
    viewModelScope.launch {
      familyGroupRepository.familyGroupInfo.collect { info ->
        val syncText =
          if (info != null) {
            val latestSync = familyGroupRepository.getLatestSyncLog()
            latestSync?.let {
              val relative =
                DateUtils.getRelativeTimeSpanString(
                    it.syncTimestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                  )
                  .toString()
              context.getString(R.string.family_group_last_sync, relative)
            }
          } else {
            null
          }
        _uiState.update { it.copy(familyGroupInfo = info, lastSyncText = syncText) }
      }
    }
  }

  fun showThemeDialog() {
    _uiState.update { it.copy(showThemeDialog = true) }
  }

  fun hideThemeDialog() {
    _uiState.update { it.copy(showThemeDialog = false) }
  }

  fun showLanguageDialog() {
    _uiState.update { it.copy(showLanguageDialog = true) }
  }

  fun hideLanguageDialog() {
    _uiState.update { it.copy(showLanguageDialog = false) }
  }

  fun updateTheme(theme: AppTheme) {
    viewModelScope.launch {
      userPreferencesRepository.updateTheme(theme)
      hideThemeDialog()
    }
  }

  fun updateLanguage(language: AppLanguage) {
    viewModelScope.launch {
      userPreferencesRepository.updateLanguage(language)
      LocaleHelper.applyLanguage(context, language)
      hideLanguageDialog()
    }
  }

  fun showDeleteAllDataDialog() {
    _uiState.update { it.copy(showDeleteAllDataDialog = true) }
  }

  fun hideDeleteAllDataDialog() {
    _uiState.update { it.copy(showDeleteAllDataDialog = false) }
  }

  fun deleteAllData() {
    viewModelScope.launch {
      _uiState.update { it.copy(isDeletingAllData = true) }
      deleteAllDataUseCase
        .execute()
        .onSuccess {
          _uiState.update { it.copy(isDeletingAllData = false, showDeleteAllDataDialog = false) }
          _events.emit(SettingsEvent.DeleteAllDataSuccess)
        }
        .onFailure { error ->
          _uiState.update { it.copy(isDeletingAllData = false, showDeleteAllDataDialog = false) }
          _events.emit(
            SettingsEvent.DeleteAllDataError(
              error.message ?: context.getString(R.string.error_unknown)
            )
          )
        }
    }
  }
}
