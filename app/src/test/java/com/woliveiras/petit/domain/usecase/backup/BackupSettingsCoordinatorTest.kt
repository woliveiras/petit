package com.woliveiras.petit.domain.usecase.backup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.BackupSettings
import com.woliveiras.petit.data.repository.BackupSettingsRepository
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import com.woliveiras.petit.worker.BackupScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BackupSettingsCoordinatorTest {
  @Test
  fun enableNetworkChangeAndDisableReconcileOneAuthoritativeSchedule() = runTest {
    val repository = FakeBackupSettingsRepository()
    val scheduler = RecordingBackupScheduler()
    val coordinator = BackupSettingsCoordinator(repository, scheduler)

    coordinator.setAutomaticBackupEnabled(true)
    coordinator.setNetworkRequirement(BackupNetworkRequirement.CONNECTED)
    coordinator.setAutomaticBackupEnabled(false)

    assertThat(scheduler.scheduled)
      .containsExactly(BackupNetworkRequirement.UNMETERED, BackupNetworkRequirement.CONNECTED)
      .inOrder()
    assertThat(scheduler.cancelCalls).isEqualTo(1)
    assertThat(repository.state.value)
      .isEqualTo(
        BackupSettings(
          automaticBackupEnabled = false,
          networkRequirement = BackupNetworkRequirement.CONNECTED,
        )
      )
  }

  @Test
  fun scheduleFailureRollsBackThePersistedPreferenceAndPreviousSchedule() = runTest {
    val repository = FakeBackupSettingsRepository()
    val scheduler = RecordingBackupScheduler(failNextSchedule = true)
    val coordinator = BackupSettingsCoordinator(repository, scheduler)

    val result = runCatching { coordinator.setAutomaticBackupEnabled(true) }

    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    assertThat(repository.state.value).isEqualTo(BackupSettings())
    assertThat(scheduler.cancelCalls).isEqualTo(1)
  }

  internal class FakeBackupSettingsRepository(initial: BackupSettings = BackupSettings()) :
    BackupSettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<BackupSettings> = state

    override suspend fun getSettings(): BackupSettings = state.value

    override suspend fun updateAutomaticBackupEnabled(enabled: Boolean) {
      state.value = state.value.copy(automaticBackupEnabled = enabled)
    }

    override suspend fun updateNetworkRequirement(requirement: BackupNetworkRequirement) {
      state.value = state.value.copy(networkRequirement = requirement)
    }

    override suspend fun updateNotifyAfterSuccess(enabled: Boolean) {
      state.value = state.value.copy(notifyAfterSuccess = enabled)
    }
  }

  internal class RecordingBackupScheduler(private var failNextSchedule: Boolean = false) :
    BackupScheduler {
    val scheduled = mutableListOf<BackupNetworkRequirement>()
    var cancelCalls = 0

    override fun schedulePeriodic(networkRequirement: BackupNetworkRequirement) {
      if (failNextSchedule) {
        failNextSchedule = false
        error("WorkManager unavailable")
      }
      scheduled += networkRequirement
    }

    override fun cancelPeriodic() {
      cancelCalls += 1
    }
  }
}
