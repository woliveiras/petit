package com.woliveiras.petit.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupSettingsRepositoryTest {

  @Test
  fun defaultsArePrivateAndUpdatesPersistAcrossRepositoryInstances() = runTest {
    val file = File.createTempFile("backup-settings-", ".preferences_pb").also { it.delete() }
    val firstJob = SupervisorJob()
    val dataStoreScope = CoroutineScope(StandardTestDispatcher(testScheduler) + firstJob)
    try {
      val dataStore =
        PreferenceDataStoreFactory.create(scope = dataStoreScope, produceFile = { file })
      val repository = BackupSettingsRepositoryImpl(dataStore)

      assertThat(repository.settings.first()).isEqualTo(BackupSettings())

      repository.updateAutomaticBackupEnabled(true)
      repository.updateNetworkRequirement(BackupNetworkRequirement.CONNECTED)
      repository.updateNotifyAfterSuccess(true)

      firstJob.cancelAndJoin()
      advanceUntilIdle()
      val reopenedScope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
      val reopened =
        BackupSettingsRepositoryImpl(
          PreferenceDataStoreFactory.create(scope = reopenedScope, produceFile = { file })
        )
      assertThat(reopened.settings.first())
        .isEqualTo(
          BackupSettings(
            automaticBackupEnabled = true,
            networkRequirement = BackupNetworkRequirement.CONNECTED,
            notifyAfterSuccess = true,
          )
        )
      reopenedScope.cancel()
    } finally {
      file.delete()
    }
  }
}
