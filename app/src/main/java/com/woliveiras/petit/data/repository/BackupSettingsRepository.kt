package com.woliveiras.petit.data.repository

import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import kotlinx.coroutines.flow.Flow

data class BackupSettings(
  val automaticBackupEnabled: Boolean = false,
  val networkRequirement: BackupNetworkRequirement = BackupNetworkRequirement.UNMETERED,
  val notifyAfterSuccess: Boolean = false,
)

interface BackupSettingsRepository {
  val settings: Flow<BackupSettings>

  suspend fun getSettings(): BackupSettings

  suspend fun updateAutomaticBackupEnabled(enabled: Boolean)

  suspend fun updateNetworkRequirement(requirement: BackupNetworkRequirement)

  suspend fun updateNotifyAfterSuccess(enabled: Boolean)
}
