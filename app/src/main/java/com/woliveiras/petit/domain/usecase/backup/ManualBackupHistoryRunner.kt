package com.woliveiras.petit.domain.usecase.backup

import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptRepository
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.data.repository.BackupFailureCategory
import com.woliveiras.petit.domain.backup.BackupTrigger
import java.time.Clock
import java.util.concurrent.CancellationException
import javax.inject.Inject

class ManualBackupHistoryRunner
@Inject
constructor(
  private val createBackupAction: CreateBackupAction,
  private val attemptRepository: BackupAttemptRepository,
  private val clock: Clock,
) {
  suspend fun run(backupId: String): BackupAttemptStatus {
    val startedAt = clock.instant()
    attemptRepository.upsert(
      BackupAttempt(
        id = backupId,
        trigger = BackupTrigger.MANUAL,
        startedAt = startedAt,
        status = BackupAttemptStatus.RUNNING,
      )
    )
    return try {
      when (val result = createBackupAction.execute(backupId, BackupTrigger.MANUAL)) {
        is BackupCreationResult.Success -> {
          record(
            BackupAttempt(
              id = backupId,
              trigger = BackupTrigger.MANUAL,
              startedAt = startedAt,
              completedAt = clock.instant(),
              status = BackupAttemptStatus.SUCCEEDED,
              archiveSizeBytes = result.metadata.archiveSizeBytes,
              contentCounts = result.metadata.contentCounts,
            )
          )
        }
        BackupCreationResult.AuthorizationRequired ->
          recordFailure(
            backupId,
            startedAt,
            BackupAttemptStatus.AUTHORIZATION_REQUIRED,
            BackupFailureCategory.AUTHORIZATION_REQUIRED,
          )
        BackupCreationResult.QuotaExceeded ->
          recordFailure(
            backupId,
            startedAt,
            BackupAttemptStatus.FAILED,
            BackupFailureCategory.QUOTA_EXCEEDED,
          )
        is BackupCreationResult.RetryableFailure ->
          recordFailure(
            backupId,
            startedAt,
            BackupAttemptStatus.RETRYING,
            BackupFailureCategory.RETRYABLE,
            completed = false,
          )
        is BackupCreationResult.PermanentFailure ->
          recordFailure(
            backupId,
            startedAt,
            BackupAttemptStatus.FAILED,
            BackupFailureCategory.PERMANENT,
          )
      }
    } catch (cancellation: CancellationException) {
      recordFailure(backupId, startedAt, BackupAttemptStatus.CANCELLED, category = null)
      throw cancellation
    } catch (_: Exception) {
      recordFailure(
        backupId,
        startedAt,
        BackupAttemptStatus.FAILED,
        BackupFailureCategory.PERMANENT,
      )
    }
  }

  private suspend fun recordFailure(
    backupId: String,
    startedAt: java.time.Instant,
    status: BackupAttemptStatus,
    category: BackupFailureCategory?,
    completed: Boolean = true,
  ): BackupAttemptStatus =
    record(
      BackupAttempt(
        id = backupId,
        trigger = BackupTrigger.MANUAL,
        startedAt = startedAt,
        completedAt = clock.instant().takeIf { completed },
        status = status,
        failureCategory = category,
      )
    )

  private suspend fun record(attempt: BackupAttempt): BackupAttemptStatus {
    attemptRepository.upsert(attempt)
    return attempt.status
  }
}
