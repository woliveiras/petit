package com.woliveiras.petit.domain.backup

import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

const val PETIT_BACKUP_CONTRACT_ID = "com.woliveiras.petit.backup"

enum class BackupTrigger {
  MANUAL,
  AUTOMATIC,
  DATA_CHANGE,
}

enum class BackupCompatibility {
  COMPATIBLE,
  ARCHIVE_VERSION_TOO_NEW,
  SCHEMA_VERSION_TOO_NEW,
  INVALID,
}

data class BackupContentCounts(
  val pets: Int = 0,
  val weights: Int = 0,
  val vaccinations: Int = 0,
  val dewormingRecords: Int = 0,
  val tasks: Int = 0,
  val assets: Int = 0,
)

data class BackupMetadata(
  val remoteId: String,
  val backupId: String,
  val createdAt: Instant,
  val trigger: BackupTrigger,
  val appVersion: String,
  val archiveFormatVersion: Int,
  val schemaVersion: Int,
  val contentCounts: BackupContentCounts,
  val archiveSizeBytes: Long,
  val archiveSha256: String,
  val contractId: String = PETIT_BACKUP_CONTRACT_ID,
  val compatibility: BackupCompatibility = BackupCompatibility.COMPATIBLE,
) {
  init {
    require(backupId.isNotBlank()) { "Backup ID cannot be blank" }
    require(archiveSizeBytes >= 0) { "Archive size cannot be negative" }
  }

  val isRecognized: Boolean
    get() =
      contractId == PETIT_BACKUP_CONTRACT_ID &&
        remoteId.isNotBlank() &&
        appVersion.isNotBlank() &&
        archiveSha256.isNotBlank() &&
        contentCounts.allNonNegative()
}

private fun BackupContentCounts.allNonNegative(): Boolean =
  pets >= 0 &&
    weights >= 0 &&
    vaccinations >= 0 &&
    dewormingRecords >= 0 &&
    tasks >= 0 &&
    assets >= 0

data class BackupPage(val backups: List<BackupMetadata>, val nextPageToken: String?)

data class BackupProgress(val bytesTransferred: Long, val totalBytes: Long) {
  init {
    require(bytesTransferred >= 0) { "Transferred bytes cannot be negative" }
    require(totalBytes >= 0) { "Total bytes cannot be negative" }
    require(bytesTransferred <= totalBytes) { "Transferred bytes cannot exceed total bytes" }
  }
}

interface BackupContent {
  val byteSize: Long

  fun openInputStream(): InputStream
}

interface BackupDownloadTarget {
  fun openOutputStream(): OutputStream
}

data class BackupUploadRequest(
  val backupId: String,
  val content: BackupContent,
  val metadata: BackupMetadata,
) {
  init {
    require(backupId == metadata.backupId) { "Upload and metadata backup IDs must match" }
    require(content.byteSize == metadata.archiveSizeBytes) {
      "Upload content size must match archive metadata"
    }
  }
}

data class BackupUploadResult(val remoteId: String, val metadata: BackupMetadata)

data class BackupDownloadResult(val metadata: BackupMetadata, val bytesDownloaded: Long)

sealed interface BackupAuthorizationState {
  data object Disconnected : BackupAuthorizationState

  data object Authorizing : BackupAuthorizationState

  data class Authorized(val accountLabel: String? = null) : BackupAuthorizationState

  data object AuthorizationRequired : BackupAuthorizationState

  data class Unavailable(val reason: String? = null) : BackupAuthorizationState
}

sealed interface BackupAuthorizationResult {
  data object Authorized : BackupAuthorizationResult

  data object Cancelled : BackupAuthorizationResult

  data class Unavailable(val reason: String? = null) : BackupAuthorizationResult
}

sealed class BackupProviderException(message: String, cause: Throwable? = null) :
  Exception(message, cause) {
  class AuthorizationRequired(cause: Throwable? = null) :
    BackupProviderException("Backup storage authorization is required", cause)

  class QuotaExceeded(cause: Throwable? = null) :
    BackupProviderException("Backup storage quota was exceeded", cause)

  class Retryable(message: String, cause: Throwable? = null) :
    BackupProviderException(message, cause)

  class Permanent(message: String, cause: Throwable? = null) :
    BackupProviderException(message, cause)

  class Interrupted(val bytesTransferred: Long) :
    BackupProviderException("Backup transfer was interrupted after $bytesTransferred bytes")
}
