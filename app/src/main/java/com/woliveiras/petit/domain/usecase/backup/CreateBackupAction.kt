package com.woliveiras.petit.domain.usecase.backup

import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupTrigger

interface CreateBackupAction {
  suspend fun execute(
    backupId: String,
    trigger: BackupTrigger,
    onProgress: (BackupProgress) -> Unit = {},
  ): BackupCreationResult
}

sealed interface BackupCreationResult {
  data class Success(val metadata: BackupMetadata) : BackupCreationResult

  data object AuthorizationRequired : BackupCreationResult

  data object QuotaExceeded : BackupCreationResult

  data class RetryableFailure(val message: String) : BackupCreationResult

  data class PermanentFailure(val message: String) : BackupCreationResult
}
