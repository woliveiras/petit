package com.woliveiras.petit.domain.usecase.backup

import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupTrigger
import javax.inject.Inject

/** Safe production placeholder until a real user-owned storage adapter is configured. */
class ProviderUnavailableCreateBackupAction @Inject constructor() : CreateBackupAction {
  override suspend fun execute(
    backupId: String,
    trigger: BackupTrigger,
    onProgress: (BackupProgress) -> Unit,
  ): BackupCreationResult =
    BackupCreationResult.PermanentFailure("Backup storage provider is unavailable")
}
