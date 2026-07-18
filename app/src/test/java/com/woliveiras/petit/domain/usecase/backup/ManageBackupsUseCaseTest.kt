package com.woliveiras.petit.domain.usecase.backup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.backup.testing.DeterministicBackupStorageGateway
import com.woliveiras.petit.domain.backup.BackupCompatibility
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProviderException
import com.woliveiras.petit.domain.backup.BackupTrigger
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ManageBackupsUseCaseTest {
  @Test
  fun listingConsumesEveryPageFiltersUnknownFilesSortsAndTotals() = runTest {
    val gateway = DeterministicBackupStorageGateway()
    gateway.seed(metadata("older", "2026-07-16T10:00:00Z", 2), ByteArray(2))
    gateway.seed(
      metadata("unknown", "2026-07-18T10:00:00Z", 8).copy(contractId = "other"),
      ByteArray(8),
    )
    gateway.seed(metadata("newer", "2026-07-17T10:00:00Z", 3), ByteArray(3))
    val useCase = ManageBackupsUseCase(gateway, 1, 1)

    val collection = useCase.listAll(pageSize = 1)

    assertThat(collection.backups.map { it.backupId }).containsExactly("newer", "older").inOrder()
    assertThat(collection.totalArchiveSizeBytes).isEqualTo(5)
  }

  @Test
  fun detailsComputesCompatibilityFromVersionsInsteadOfProviderFilename() = runTest {
    val gateway = DeterministicBackupStorageGateway()
    gateway.seed(metadata("archive-new", archiveVersion = 2), ByteArray(1))
    gateway.seed(metadata("schema-new", schemaVersion = 2), ByteArray(1))
    val useCase = ManageBackupsUseCase(gateway, 1, 1)

    val archiveNew = useCase.details("remote-archive-new")
    val schemaNew = useCase.details("remote-schema-new")

    assertThat(archiveNew?.compatibility).isEqualTo(BackupCompatibility.ARCHIVE_VERSION_TOO_NEW)
    assertThat(schemaNew?.compatibility).isEqualTo(BackupCompatibility.SCHEMA_VERSION_TOO_NEW)
  }

  @Test
  fun listingRejectsMalformedProviderMetadataEvenWithThePetitContractId() = runTest {
    val gateway = DeterministicBackupStorageGateway()
    gateway.seed(metadata("valid"), ByteArray(1))
    gateway.seed(metadata("blank-remote").copy(remoteId = ""), ByteArray(1))
    gateway.seed(metadata("blank-app").copy(appVersion = ""), ByteArray(1))
    gateway.seed(metadata("blank-checksum").copy(archiveSha256 = ""), ByteArray(1))
    gateway.seed(
      metadata("negative-count").copy(contentCounts = BackupContentCounts(pets = -1)),
      ByteArray(1),
    )
    val useCase = ManageBackupsUseCase(gateway, 1, 1)

    val collection = useCase.listAll()

    assertThat(collection.backups.map { it.backupId }).containsExactly("valid")
  }

  @Test
  fun exactDeletionIsIdempotentAndNeverDeletesUnrecognizedFile() = runTest {
    val gateway = DeterministicBackupStorageGateway()
    gateway.seed(metadata("kept").copy(contractId = "other"), ByteArray(1))
    gateway.seed(metadata("deleted"), ByteArray(1))
    val useCase = ManageBackupsUseCase(gateway, 1, 1)

    val first = useCase.deleteOne("remote-deleted")
    val retry = useCase.deleteOne("remote-deleted")
    val unknown = useCase.deleteOne("remote-kept")

    assertThat(first.deletedRemoteIds).containsExactly("remote-deleted")
    assertThat(retry.deletedRemoteIds).containsExactly("remote-deleted")
    assertThat(unknown.failures.single().category)
      .isEqualTo(BackupDeletionFailureCategory.UNRECOGNIZED)
    assertThat(gateway.completedBackups().map { it.backupId }).containsExactly("kept")
  }

  @Test
  fun bulkDeletionReportsPartialFailureAndRetryOnlyTargetsFailures() = runTest {
    val gateway = DeterministicBackupStorageGateway()
    gateway.seed(metadata("one"), ByteArray(1))
    gateway.seed(metadata("two"), ByteArray(1))
    gateway.seed(metadata("three"), ByteArray(1))
    val useCase = ManageBackupsUseCase(gateway, 1, 1)
    gateway.failDeleteForRemoteIdOnce("remote-two", BackupProviderException.Retryable("offline"))

    val partial = useCase.deleteSelected(setOf("remote-one", "remote-two", "remote-three"))
    val retry = useCase.retryDeletion(partial)

    assertThat(partial.deletedRemoteIds).containsExactly("remote-one", "remote-three")
    assertThat(partial.failures.single().remoteId).isEqualTo("remote-two")
    assertThat(partial.failures.single().category)
      .isEqualTo(BackupDeletionFailureCategory.RETRYABLE)
    assertThat(retry.deletedRemoteIds).containsExactly("remote-two")
    assertThat(gateway.completedBackups()).isEmpty()
  }

  @Test
  fun deleteAllResolvesRecognizedExactIdsAndLeavesUnknownFiles() = runTest {
    val gateway = DeterministicBackupStorageGateway()
    gateway.seed(metadata("one"), ByteArray(1))
    gateway.seed(metadata("two"), ByteArray(1))
    gateway.seed(metadata("unknown").copy(contractId = "other"), ByteArray(1))
    val useCase = ManageBackupsUseCase(gateway, 1, 1)

    val result = useCase.deleteAll()

    assertThat(result.deletedRemoteIds).containsExactly("remote-one", "remote-two")
    assertThat(gateway.completedBackups().map { it.backupId }).containsExactly("unknown")
  }

  private fun metadata(
    id: String,
    createdAt: String = "2026-07-18T10:00:00Z",
    size: Long = 1,
    archiveVersion: Int = 1,
    schemaVersion: Int = 1,
  ) =
    BackupMetadata(
      remoteId = "remote-$id",
      backupId = id,
      createdAt = Instant.parse(createdAt),
      trigger = BackupTrigger.MANUAL,
      appVersion = "1.0.0",
      archiveFormatVersion = archiveVersion,
      schemaVersion = schemaVersion,
      contentCounts = BackupContentCounts(pets = 1),
      archiveSizeBytes = size,
      archiveSha256 = "sha256-$id",
    )
}
