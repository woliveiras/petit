package com.woliveiras.petit.data.backup

import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Production-safe boundary used until a real user-owned storage adapter is installed. */
@Singleton
class ProviderUnavailableBackupAuthorizationGateway @Inject constructor() :
  BackupAuthorizationGateway {
  private val mutableState =
    MutableStateFlow<BackupAuthorizationState>(BackupAuthorizationState.Unavailable())

  override val state: StateFlow<BackupAuthorizationState> = mutableState

  override suspend fun authorize(): BackupAuthorizationResult =
    BackupAuthorizationResult.Unavailable()

  override suspend fun disconnect() = Unit
}
