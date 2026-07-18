package com.woliveiras.petit.presentation.feature.backup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptRepository
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.usecase.backup.BackupConnectionCoordinator
import com.woliveiras.petit.domain.usecase.backup.BackupCreationResult
import com.woliveiras.petit.domain.usecase.backup.BackupSettingsCoordinator
import com.woliveiras.petit.domain.usecase.backup.BackupSettingsCoordinatorTest.FakeBackupSettingsRepository
import com.woliveiras.petit.domain.usecase.backup.BackupSettingsCoordinatorTest.RecordingBackupScheduler
import com.woliveiras.petit.domain.usecase.backup.CreateBackupAction
import com.woliveiras.petit.domain.usecase.backup.ManualBackupHistoryRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupSettingsViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun stateCombinesIndependentAuthorizationPreferencesAndNonClinicalHistory() =
    runTest(dispatcher) {
      val fixture = fixture()
      fixture.authorization.mutableState.value = BackupAuthorizationState.Authorized("Caregiver")
      fixture.settings.updateNotifyAfterSuccess(true)
      fixture.history.upsert(attempt("existing", BackupAttemptStatus.SUCCEEDED))

      advanceUntilIdle()

      assertThat(fixture.viewModel.uiState.value.authorization)
        .isEqualTo(BackupAuthorizationState.Authorized("Caregiver"))
      assertThat(fixture.viewModel.uiState.value.settings.notifyAfterSuccess).isTrue()
      assertThat(fixture.viewModel.uiState.value.attempts.map { it.id }).containsExactly("existing")
    }

  @Test
  fun manualBackupRecordsHistoryWithoutReplacingOrCancellingPeriodicWork() =
    runTest(dispatcher) {
      val fixture = fixture()

      fixture.viewModel.backUpNow()
      advanceUntilIdle()

      assertThat(fixture.action.calls).containsExactly("manual-1" to BackupTrigger.MANUAL)
      assertThat(fixture.scheduler.scheduled).isEmpty()
      assertThat(fixture.scheduler.cancelCalls).isEqualTo(0)
      assertThat(fixture.viewModel.uiState.value.manualAttemptStatus)
        .isEqualTo(BackupAttemptStatus.SUCCEEDED)
      assertThat(fixture.history.current.single().trigger).isEqualTo(BackupTrigger.MANUAL)
    }

  @Test
  fun disconnectCancelsAutomaticWorkAndPreservesTheProviderDeletionBoundary() =
    runTest(dispatcher) {
      val fixture = fixture()
      fixture.viewModel.setAutomaticBackupEnabled(true)
      advanceUntilIdle()

      fixture.viewModel.disconnect()
      advanceUntilIdle()

      assertThat(fixture.settings.state.value.automaticBackupEnabled).isFalse()
      assertThat(fixture.scheduler.scheduled).containsExactly(BackupNetworkRequirement.UNMETERED)
      assertThat(fixture.scheduler.cancelCalls).isEqualTo(1)
      assertThat(fixture.authorization.disconnectCalls).isEqualTo(1)
      assertThat(fixture.authorization.state.value).isEqualTo(BackupAuthorizationState.Disconnected)
    }

  private fun fixture(): Fixture {
    val settings = FakeBackupSettingsRepository()
    val scheduler = RecordingBackupScheduler()
    val settingsCoordinator = BackupSettingsCoordinator(settings, scheduler)
    val authorization = FakeAuthorizationGateway()
    val connection = BackupConnectionCoordinator(authorization, settingsCoordinator)
    val history = RecordingAttemptRepository()
    val action = FakeCreateBackupAction()
    val manualRunner =
      ManualBackupHistoryRunner(
        action,
        history,
        Clock.fixed(Instant.parse("2026-07-18T08:00:00Z"), ZoneOffset.UTC),
      )
    val viewModel =
      BackupSettingsViewModel(
        settings,
        history,
        settingsCoordinator,
        connection,
        manualRunner,
        authorization.state,
        backupIdFactory = { "manual-1" },
      )
    return Fixture(viewModel, settings, scheduler, authorization, history, action)
  }

  private data class Fixture(
    val viewModel: BackupSettingsViewModel,
    val settings: FakeBackupSettingsRepository,
    val scheduler: RecordingBackupScheduler,
    val authorization: FakeAuthorizationGateway,
    val history: RecordingAttemptRepository,
    val action: FakeCreateBackupAction,
  )

  private class FakeAuthorizationGateway : BackupAuthorizationGateway {
    val mutableState =
      MutableStateFlow<BackupAuthorizationState>(BackupAuthorizationState.Authorized())
    override val state: StateFlow<BackupAuthorizationState> = mutableState
    var disconnectCalls = 0

    override suspend fun authorize(): BackupAuthorizationResult =
      BackupAuthorizationResult.Authorized

    override suspend fun disconnect() {
      disconnectCalls += 1
      mutableState.value = BackupAuthorizationState.Disconnected
    }
  }

  private class RecordingAttemptRepository : BackupAttemptRepository {
    private val state = MutableStateFlow<List<BackupAttempt>>(emptyList())
    val current: List<BackupAttempt>
      get() = state.value

    override val attempts: Flow<List<BackupAttempt>> = state

    override suspend fun getAttempt(id: String): BackupAttempt? =
      state.value.firstOrNull { it.id == id }

    override suspend fun upsert(attempt: BackupAttempt) {
      state.value = state.value.filterNot { it.id == attempt.id } + attempt
    }
  }

  private class FakeCreateBackupAction : CreateBackupAction {
    val calls = mutableListOf<Pair<String, BackupTrigger>>()

    override suspend fun execute(
      backupId: String,
      trigger: BackupTrigger,
      onProgress: (BackupProgress) -> Unit,
    ): BackupCreationResult {
      calls += backupId to trigger
      return BackupCreationResult.Success(metadata(backupId))
    }
  }

  companion object {
    private fun attempt(id: String, status: BackupAttemptStatus) =
      BackupAttempt(
        id = id,
        trigger = BackupTrigger.AUTOMATIC,
        startedAt = Instant.EPOCH,
        completedAt = Instant.EPOCH,
        status = status,
      )

    private fun metadata(backupId: String) =
      BackupMetadata(
        remoteId = "remote-$backupId",
        backupId = backupId,
        createdAt = Instant.EPOCH,
        trigger = BackupTrigger.MANUAL,
        appVersion = "1.0",
        archiveFormatVersion = 1,
        schemaVersion = 1,
        contentCounts = BackupContentCounts(pets = 1),
        archiveSizeBytes = 1,
        archiveSha256 = "checksum",
      )
  }
}
