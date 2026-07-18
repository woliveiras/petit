package com.woliveiras.petit.data.backup.testing

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProviderException
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.backup.BackupUploadRequest
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeterministicBackupStorageGatewayTest {
  @Test
  fun uploadReportsMonotonicByteProgressAndRetryIsIdempotent() = runTest {
    val gateway = DeterministicBackupStorageGateway(uploadChunkSize = 3)
    val progress = mutableListOf<Long>()
    val request = request(backupId = "backup-1", bytes = "portable archive".encodeToByteArray())

    val first = gateway.upload(request) { progress += it.bytesTransferred }
    val retry = gateway.upload(request) { progress += it.bytesTransferred }

    assertThat(progress).isInOrder()
    assertThat(progress.last()).isEqualTo(request.content.byteSize)
    assertThat(retry.remoteId).isEqualTo(first.remoteId)
    assertThat(gateway.completedBackups()).hasSize(1)
  }

  @Test
  fun retryAfterUnknownCommitOutcomeReturnsTheSingleCompletedBackup() = runTest {
    val gateway = DeterministicBackupStorageGateway(uploadChunkSize = 3)
    val request = request(backupId = "backup-1", bytes = "archive".encodeToByteArray())
    gateway.failNextUploadAfterCommit(BackupProviderException.Retryable("response lost"))

    runCatching { gateway.upload(request) }
    val retry = gateway.upload(request)

    assertThat(retry.metadata.backupId).isEqualTo("backup-1")
    assertThat(gateway.completedBackups()).containsExactly(retry.metadata)
  }

  @Test
  fun zeroByteUploadStillReportsACompleteProgressEvent() = runTest {
    val gateway = DeterministicBackupStorageGateway()
    val progress = mutableListOf<Long>()

    gateway.upload(request("empty", ByteArray(0))) { progress += it.bytesTransferred }

    assertThat(progress).containsExactly(0L)
  }

  private fun request(backupId: String, bytes: ByteArray): BackupUploadRequest {
    return BackupUploadRequest(
      backupId = backupId,
      content = ByteArrayBackupContent(bytes),
      metadata =
        BackupMetadata(
          remoteId = "",
          backupId = backupId,
          createdAt = Instant.parse("2026-07-18T10:00:00Z"),
          trigger = BackupTrigger.MANUAL,
          appVersion = "1.0.0",
          archiveFormatVersion = 1,
          schemaVersion = 1,
          contentCounts = BackupContentCounts(pets = 2),
          archiveSizeBytes = bytes.size.toLong(),
          archiveSha256 = "sha256",
        ),
    )
  }
}
