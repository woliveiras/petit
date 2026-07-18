package com.woliveiras.petit.domain.usecase.backup

import com.woliveiras.petit.domain.backup.BackupCompatibility
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupProviderException
import com.woliveiras.petit.domain.backup.BackupStorageGateway
import java.util.concurrent.CancellationException

data class SavedBackupCollection(val backups: List<BackupMetadata>, val totalArchiveSizeBytes: Long)

data class BackupDeletionResult(
  val deletedRemoteIds: Set<String>,
  val failures: List<BackupDeletionFailure>,
) {
  val isComplete: Boolean
    get() = failures.isEmpty()
}

data class BackupDeletionFailure(val remoteId: String, val category: BackupDeletionFailureCategory)

enum class BackupDeletionFailureCategory {
  AUTHORIZATION_REQUIRED,
  QUOTA_EXCEEDED,
  RETRYABLE,
  PERMANENT,
  UNRECOGNIZED,
}

class ManageBackupsUseCase(
  private val storageGateway: BackupStorageGateway,
  private val supportedArchiveFormatVersion: Int,
  private val supportedSchemaVersion: Int,
) {
  init {
    require(supportedArchiveFormatVersion > 0) { "Supported archive version must be positive" }
    require(supportedSchemaVersion > 0) { "Supported schema version must be positive" }
  }

  suspend fun listAll(pageSize: Int = DEFAULT_PAGE_SIZE): SavedBackupCollection {
    require(pageSize in 1..MAX_PAGE_SIZE) { "Page size is out of bounds" }
    val all = mutableListOf<BackupMetadata>()
    val seenTokens = mutableSetOf<String>()
    var pageToken: String? = null
    var pageCount = 0
    do {
      check(pageCount++ < MAX_PAGE_COUNT) { "Backup pagination exceeded its safety limit" }
      val page = storageGateway.list(pageToken, pageSize)
      all += page.backups.filter(BackupMetadata::isRecognized).map(::withCompatibility)
      pageToken = page.nextPageToken
      check(pageToken == null || seenTokens.add(pageToken)) { "Backup pagination token repeated" }
    } while (pageToken != null)

    val sorted = all.distinctBy(BackupMetadata::remoteId).sortedWith(BACKUP_ORDER)
    val total = sorted.fold(0L) { sum, backup -> Math.addExact(sum, backup.archiveSizeBytes) }
    return SavedBackupCollection(sorted, total)
  }

  suspend fun details(remoteId: String): BackupMetadata? {
    require(remoteId.isNotBlank()) { "Remote backup ID cannot be blank" }
    return storageGateway
      .get(remoteId)
      ?.takeIf(BackupMetadata::isRecognized)
      ?.let(::withCompatibility)
  }

  suspend fun deleteOne(remoteId: String): BackupDeletionResult = deleteSelected(setOf(remoteId))

  suspend fun deleteSelected(remoteIds: Set<String>): BackupDeletionResult {
    require(remoteIds.none(String::isBlank)) { "Remote backup IDs cannot be blank" }
    val deleted = linkedSetOf<String>()
    val failures = mutableListOf<BackupDeletionFailure>()
    remoteIds.sorted().forEach { remoteId ->
      try {
        val resolved = storageGateway.get(remoteId)
        when {
          resolved == null -> deleted += remoteId
          !resolved.isRecognized ->
            failures += BackupDeletionFailure(remoteId, BackupDeletionFailureCategory.UNRECOGNIZED)
          else -> {
            storageGateway.deleteExact(remoteId)
            deleted += remoteId
          }
        }
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (_: BackupProviderException.AuthorizationRequired) {
        failures +=
          BackupDeletionFailure(remoteId, BackupDeletionFailureCategory.AUTHORIZATION_REQUIRED)
      } catch (_: BackupProviderException.QuotaExceeded) {
        failures += BackupDeletionFailure(remoteId, BackupDeletionFailureCategory.QUOTA_EXCEEDED)
      } catch (_: BackupProviderException.Retryable) {
        failures += BackupDeletionFailure(remoteId, BackupDeletionFailureCategory.RETRYABLE)
      } catch (_: BackupProviderException.Permanent) {
        failures += BackupDeletionFailure(remoteId, BackupDeletionFailureCategory.PERMANENT)
      } catch (_: Exception) {
        failures += BackupDeletionFailure(remoteId, BackupDeletionFailureCategory.PERMANENT)
      }
    }
    return BackupDeletionResult(deleted, failures)
  }

  suspend fun deleteAll(): BackupDeletionResult {
    val resolved = listAll()
    return deleteSelected(resolved.backups.mapTo(linkedSetOf(), BackupMetadata::remoteId))
  }

  suspend fun retryDeletion(previous: BackupDeletionResult): BackupDeletionResult {
    return deleteSelected(previous.failures.mapTo(linkedSetOf(), BackupDeletionFailure::remoteId))
  }

  private fun withCompatibility(metadata: BackupMetadata): BackupMetadata {
    val compatibility =
      when {
        metadata.archiveFormatVersion <= 0 || metadata.schemaVersion <= 0 ->
          BackupCompatibility.INVALID
        metadata.archiveFormatVersion > supportedArchiveFormatVersion ->
          BackupCompatibility.ARCHIVE_VERSION_TOO_NEW
        metadata.schemaVersion > supportedSchemaVersion ->
          BackupCompatibility.SCHEMA_VERSION_TOO_NEW
        else -> BackupCompatibility.COMPATIBLE
      }
    return metadata.copy(compatibility = compatibility)
  }

  private companion object {
    const val DEFAULT_PAGE_SIZE = 50
    const val MAX_PAGE_SIZE = 100
    const val MAX_PAGE_COUNT = 10_000
    val BACKUP_ORDER = compareByDescending<BackupMetadata> { it.createdAt }.thenBy { it.remoteId }
  }
}
