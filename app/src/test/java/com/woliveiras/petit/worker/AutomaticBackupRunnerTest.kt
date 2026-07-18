package com.woliveiras.petit.worker

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptRepository
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.data.repository.BackupFailureCategory
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.usecase.backup.BackupCreationResult
import com.woliveiras.petit.domain.usecase.backup.CreateBackupAction
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AutomaticBackupRunnerTest {
  private val now = Instant.parse("2026-07-18T08:00:00Z")
  private val clock = Clock.fixed(now, ZoneOffset.UTC)

  @Test
  fun successRecordsNonClinicalArchiveSummary() = runTest {
    val action = FakeCreateBackupAction(mutableListOf(BackupCreationResult.Success(metadata())))
    val history = RecordingAttemptRepository()

    val outcome = AutomaticBackupRunner(action, history, clock).run("work-1")

    assertThat(outcome).isEqualTo(AutomaticBackupOutcome.SUCCESS)
    assertThat(action.backupIds).containsExactly("work-1")
    assertThat(history.records.map { it.status })
      .containsExactly(BackupAttemptStatus.RUNNING, BackupAttemptStatus.SUCCEEDED)
      .inOrder()
    assertThat(history.current.single().archiveSizeBytes).isEqualTo(256L)
    assertThat(history.current.single().contentCounts).isEqualTo(BackupContentCounts(pets = 2))
  }

  @Test
  fun retryReusesStableIdAndPreservesTheOriginalAttemptStart() = runTest {
    val action =
      FakeCreateBackupAction(
        mutableListOf(
          BackupCreationResult.RetryableFailure("offline"),
          BackupCreationResult.Success(metadata()),
        )
      )
    val history = RecordingAttemptRepository()
    val runner = AutomaticBackupRunner(action, history, clock)

    assertThat(runner.run("stable-work-id")).isEqualTo(AutomaticBackupOutcome.RETRY)
    assertThat(runner.run("stable-work-id")).isEqualTo(AutomaticBackupOutcome.SUCCESS)

    assertThat(action.backupIds).containsExactly("stable-work-id", "stable-work-id").inOrder()
    assertThat(history.records.any { it.status == BackupAttemptStatus.RETRYING }).isTrue()
    assertThat(history.current.single().startedAt).isEqualTo(now)
  }

  @Test
  fun authorizationQuotaAndPermanentFailuresRemainActionableWithoutMessages() = runTest {
    val cases =
      listOf(
        BackupCreationResult.AuthorizationRequired to
          (BackupAttemptStatus.AUTHORIZATION_REQUIRED to
            BackupFailureCategory.AUTHORIZATION_REQUIRED),
        BackupCreationResult.QuotaExceeded to
          (BackupAttemptStatus.FAILED to BackupFailureCategory.QUOTA_EXCEEDED),
        BackupCreationResult.PermanentFailure("provider detail") to
          (BackupAttemptStatus.FAILED to BackupFailureCategory.PERMANENT),
      )

    cases.forEachIndexed { index, (result, expected) ->
      val history = RecordingAttemptRepository()
      val outcome =
        AutomaticBackupRunner(FakeCreateBackupAction(mutableListOf(result)), history, clock)
          .run("work-$index")

      assertThat(outcome).isEqualTo(AutomaticBackupOutcome.FAILURE)
      assertThat(history.current.single().status).isEqualTo(expected.first)
      assertThat(history.current.single().failureCategory).isEqualTo(expected.second)
    }
  }

  @Test
  fun cancellationIsRecordedAndPropagatedToWorkManager() = runTest {
    val history = RecordingAttemptRepository()
    val action =
      object : CreateBackupAction {
        override suspend fun execute(
          backupId: String,
          trigger: BackupTrigger,
          onProgress: (BackupProgress) -> Unit,
        ): BackupCreationResult = throw CancellationException("stopped")
      }

    val failure = runCatching { AutomaticBackupRunner(action, history, clock).run("work-1") }

    assertThat(failure.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    assertThat(history.current.single().status).isEqualTo(BackupAttemptStatus.CANCELLED)
  }

  @Test
  fun unexpectedFailureIsSanitizedAndDoesNotLeaveAttemptRunning() = runTest {
    val history = RecordingAttemptRepository()
    val action =
      object : CreateBackupAction {
        override suspend fun execute(
          backupId: String,
          trigger: BackupTrigger,
          onProgress: (BackupProgress) -> Unit,
        ): BackupCreationResult = error("pet name and provider internals")
      }

    val outcome = AutomaticBackupRunner(action, history, clock).run("work-1")

    assertThat(outcome).isEqualTo(AutomaticBackupOutcome.FAILURE)
    assertThat(history.current.single().status).isEqualTo(BackupAttemptStatus.FAILED)
    assertThat(history.current.single().failureCategory).isEqualTo(BackupFailureCategory.PERMANENT)
  }

  private fun metadata() =
    BackupMetadata(
      remoteId = "remote-1",
      backupId = "work-1",
      createdAt = now,
      trigger = BackupTrigger.AUTOMATIC,
      appVersion = "1.0",
      archiveFormatVersion = 1,
      schemaVersion = 1,
      contentCounts = BackupContentCounts(pets = 2),
      archiveSizeBytes = 256,
      archiveSha256 = "abc",
    )

  private class FakeCreateBackupAction(private val results: MutableList<BackupCreationResult>) :
    CreateBackupAction {
    val backupIds = mutableListOf<String>()

    override suspend fun execute(
      backupId: String,
      trigger: BackupTrigger,
      onProgress: (BackupProgress) -> Unit,
    ): BackupCreationResult {
      backupIds += backupId
      return results.removeFirst()
    }
  }

  private class RecordingAttemptRepository : BackupAttemptRepository {
    private val state = MutableStateFlow<List<BackupAttempt>>(emptyList())
    val records = mutableListOf<BackupAttempt>()
    val current: List<BackupAttempt>
      get() = state.value

    override val attempts: Flow<List<BackupAttempt>> = state

    override suspend fun getAttempt(id: String): BackupAttempt? =
      state.value.firstOrNull { it.id == id }

    override suspend fun upsert(attempt: BackupAttempt) {
      records += attempt
      state.value = state.value.filterNot { it.id == attempt.id } + attempt
    }
  }
}
