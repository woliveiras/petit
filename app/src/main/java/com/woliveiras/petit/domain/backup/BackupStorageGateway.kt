package com.woliveiras.petit.domain.backup

/** Replaceable remote storage boundary. Domain callers never observe provider SDK types. */
interface BackupStorageGateway {
  suspend fun upload(
    request: BackupUploadRequest,
    onProgress: (BackupProgress) -> Unit = {},
  ): BackupUploadResult

  suspend fun list(pageToken: String? = null, pageSize: Int = 50): BackupPage

  suspend fun get(remoteId: String): BackupMetadata?

  suspend fun download(
    remoteId: String,
    target: BackupDownloadTarget,
    onProgress: (BackupProgress) -> Unit = {},
  ): BackupDownloadResult

  /** Deletes exactly one previously resolved provider ID. Missing files count as deleted. */
  suspend fun deleteExact(remoteId: String)
}
