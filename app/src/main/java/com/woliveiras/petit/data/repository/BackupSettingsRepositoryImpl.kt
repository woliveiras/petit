package com.woliveiras.petit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.backupSettingsDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "backup_settings")

@Singleton
class BackupSettingsRepositoryImpl
internal constructor(private val dataStore: DataStore<Preferences>) : BackupSettingsRepository {
  @Inject constructor(@ApplicationContext context: Context) : this(context.backupSettingsDataStore)

  private object PreferencesKeys {
    val AUTOMATIC_BACKUP_ENABLED = booleanPreferencesKey("automatic_backup_enabled")
    val NETWORK_REQUIREMENT = stringPreferencesKey("backup_network_requirement")
    val NOTIFY_AFTER_SUCCESS = booleanPreferencesKey("notify_after_backup_success")
  }

  override val settings: Flow<BackupSettings> =
    dataStore.data.map { preferences ->
      BackupSettings(
        automaticBackupEnabled = preferences[PreferencesKeys.AUTOMATIC_BACKUP_ENABLED] ?: false,
        networkRequirement =
          preferences[PreferencesKeys.NETWORK_REQUIREMENT]?.let { stored ->
            BackupNetworkRequirement.entries.firstOrNull { it.name == stored }
          } ?: BackupNetworkRequirement.UNMETERED,
        notifyAfterSuccess = preferences[PreferencesKeys.NOTIFY_AFTER_SUCCESS] ?: false,
      )
    }

  override suspend fun getSettings(): BackupSettings = settings.first()

  override suspend fun updateAutomaticBackupEnabled(enabled: Boolean) {
    dataStore.edit { it[PreferencesKeys.AUTOMATIC_BACKUP_ENABLED] = enabled }
  }

  override suspend fun updateNetworkRequirement(requirement: BackupNetworkRequirement) {
    dataStore.edit { it[PreferencesKeys.NETWORK_REQUIREMENT] = requirement.name }
  }

  override suspend fun updateNotifyAfterSuccess(enabled: Boolean) {
    dataStore.edit { it[PreferencesKeys.NOTIFY_AFTER_SUCCESS] = enabled }
  }
}
