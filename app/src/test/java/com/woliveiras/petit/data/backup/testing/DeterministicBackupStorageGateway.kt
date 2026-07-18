package com.woliveiras.petit.data.backup.testing

import com.woliveiras.petit.domain.backup.BackupAuthorizationGateway
import com.woliveiras.petit.domain.backup.BackupAuthorizationResult
import com.woliveiras.petit.domain.backup.BackupAuthorizationState
import com.woliveiras.petit.domain.backup.BackupContent
import com.woliveiras.petit.domain.backup.BackupDownloadResult
import com.woliveiras.petit.domain.backup.BackupDownloadTarget
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupPage
import com.woliveiras.petit.domain.backup.BackupProgress
import com.woliveiras.petit.domain.backup.BackupProviderException
import com.woliveiras.petit.domain.backup.BackupStorageGateway
import com.woliveiras.petit.domain.backup.BackupUploadRequest
import com.woliveiras.petit.domain.backup.BackupUploadResult
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.ArrayDeque
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ByteArrayBackupContent(private val bytes: ByteArray) : BackupContent {
  override val byteSize: Long = bytes.size.toLong()

  override fun openInputStream(): InputStream = ByteArrayInputStream(bytes)
}

/** Test-only provider simulator. It is never bound into a production component. */
class DeterministicBackupStorageGateway(
  private val uploadChunkSize: Int = 8 * 1024,
  initialAuthorization: BackupAuthorizationState = BackupAuthorizationState.Authorized(),
) : BackupStorageGateway, BackupAuthorizationGateway {
  private data class StoredBackup(val metadata: BackupMetadata, val bytes: ByteArray)

  private val storedByBackupId = linkedMapOf<String, StoredBackup>()
  private val failures = mutableMapOf<Operation, ArrayDeque<BackupProviderException>>()
  private val deleteFailuresByRemoteId = mutableMapOf<String, ArrayDeque<BackupProviderException>>()
  private val mutableState = MutableStateFlow(initialAuthorization)
  private var nextRemoteId = 1
  private var uploadInterruptAfterBytes: Long? = null
  private var uploadFailureAfterCommit: BackupProviderException? = null
  private var nextReportedUploadTotalBytes: Long? = null
  var nextAuthorizationResult: BackupAuthorizationResult = BackupAuthorizationResult.Authorized
  var listCalls: Int = 0
    private set

  val deleteRequests = mutableListOf<String>()

  override val state: StateFlow<BackupAuthorizationState> = mutableState

  enum class Operation {
    UPLOAD,
    LIST,
    GET,
    DOWNLOAD,
    DELETE,
  }

  init {
    require(uploadChunkSize > 0) { "Upload chunk size must be positive" }
  }

  fun setAuthorization(state: BackupAuthorizationState) {
    mutableState.value = state
  }

  fun failNext(operation: Operation, failure: BackupProviderException) {
    failures.getOrPut(operation) { ArrayDeque() }.addLast(failure)
  }

  fun failDeleteForRemoteIdOnce(remoteId: String, failure: BackupProviderException) {
    deleteFailuresByRemoteId.getOrPut(remoteId) { ArrayDeque() }.addLast(failure)
  }

  fun interruptNextUploadAfter(bytes: Long) {
    require(bytes >= 0) { "Interruption offset cannot be negative" }
    uploadInterruptAfterBytes = bytes
  }

  fun failNextUploadAfterCommit(failure: BackupProviderException) {
    uploadFailureAfterCommit = failure
  }

  fun reportNextUploadTotalBytes(totalBytes: Long) {
    require(totalBytes >= 0) { "Reported total bytes cannot be negative" }
    nextReportedUploadTotalBytes = totalBytes
  }

  fun seed(metadata: BackupMetadata, bytes: ByteArray = ByteArray(0)) {
    require(bytes.size.toLong() == metadata.archiveSizeBytes) {
      "Seed bytes must match archive metadata"
    }
    storedByBackupId[metadata.backupId] = StoredBackup(metadata, bytes.copyOf())
  }

  fun completedBackups(): List<BackupMetadata> = storedByBackupId.values.map { it.metadata }

  override suspend fun disconnect() {
    mutableState.value = BackupAuthorizationState.Disconnected
  }

  override suspend fun authorize(): BackupAuthorizationResult {
    mutableState.value = BackupAuthorizationState.Authorizing
    return nextAuthorizationResult.also { result ->
      mutableState.value =
        when (result) {
          BackupAuthorizationResult.Authorized -> BackupAuthorizationState.Authorized()
          BackupAuthorizationResult.Cancelled -> BackupAuthorizationState.AuthorizationRequired
          is BackupAuthorizationResult.Unavailable ->
            BackupAuthorizationState.Unavailable(result.reason)
        }
    }
  }

  override suspend fun upload(
    request: BackupUploadRequest,
    onProgress: (BackupProgress) -> Unit,
  ): BackupUploadResult {
    ensureAuthorized()
    throwScriptedFailure(Operation.UPLOAD)
    storedByBackupId[request.backupId]?.let {
      onProgress(BackupProgress(request.content.byteSize, request.content.byteSize))
      return BackupUploadResult(it.metadata.remoteId, it.metadata)
    }

    val output = ByteArrayOutputStream()
    var transferred = 0L
    val reportedTotal = nextReportedUploadTotalBytes ?: request.content.byteSize
    nextReportedUploadTotalBytes = null
    if (request.content.byteSize == 0L) onProgress(BackupProgress(0, reportedTotal))
    request.content.openInputStream().use { input ->
      val buffer = ByteArray(uploadChunkSize)
      while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        val interruption = uploadInterruptAfterBytes
        if (interruption != null && transferred + count > interruption) {
          val accepted = (interruption - transferred).coerceAtLeast(0).toInt()
          if (accepted > 0) output.write(buffer, 0, accepted)
          transferred += accepted
          uploadInterruptAfterBytes = null
          throw BackupProviderException.Interrupted(transferred)
        }
        output.write(buffer, 0, count)
        transferred += count
        onProgress(BackupProgress(transferred, reportedTotal))
      }
    }

    val remoteId = "fake-backup-${nextRemoteId++}"
    val metadata = request.metadata.copy(remoteId = remoteId)
    storedByBackupId[request.backupId] = StoredBackup(metadata, output.toByteArray())
    uploadFailureAfterCommit?.let { failure ->
      uploadFailureAfterCommit = null
      throw failure
    }
    return BackupUploadResult(remoteId, metadata)
  }

  override suspend fun list(pageToken: String?, pageSize: Int): BackupPage {
    ensureAuthorized()
    listCalls += 1
    throwScriptedFailure(Operation.LIST)
    require(pageSize > 0) { "Page size must be positive" }
    val offset = pageToken?.toIntOrNull() ?: 0
    require(offset >= 0) { "Invalid page token" }
    val values = storedByBackupId.values.map { it.metadata }
    val page = values.drop(offset).take(pageSize)
    val nextOffset = offset + page.size
    return BackupPage(page, nextOffset.takeIf { it < values.size }?.toString())
  }

  override suspend fun get(remoteId: String): BackupMetadata? {
    ensureAuthorized()
    throwScriptedFailure(Operation.GET)
    return storedByBackupId.values.firstOrNull { it.metadata.remoteId == remoteId }?.metadata
  }

  override suspend fun download(
    remoteId: String,
    target: BackupDownloadTarget,
    onProgress: (BackupProgress) -> Unit,
  ): BackupDownloadResult {
    ensureAuthorized()
    throwScriptedFailure(Operation.DOWNLOAD)
    val stored =
      storedByBackupId.values.firstOrNull { it.metadata.remoteId == remoteId }
        ?: throw BackupProviderException.Permanent("Backup does not exist")
    target.openOutputStream().use { output ->
      var transferred = 0L
      stored.bytes.asList().chunked(uploadChunkSize).forEach { chunk ->
        val bytes = chunk.toByteArray()
        output.write(bytes)
        transferred += bytes.size
        onProgress(BackupProgress(transferred, stored.bytes.size.toLong()))
      }
    }
    return BackupDownloadResult(stored.metadata, stored.bytes.size.toLong())
  }

  override suspend fun deleteExact(remoteId: String) {
    ensureAuthorized()
    deleteRequests += remoteId
    throwScriptedFailure(Operation.DELETE)
    deleteFailuresByRemoteId[remoteId]?.pollFirst()?.let { throw it }
    val entry = storedByBackupId.entries.firstOrNull { it.value.metadata.remoteId == remoteId }
    if (entry != null) storedByBackupId.remove(entry.key)
  }

  private fun ensureAuthorized() {
    if (mutableState.value !is BackupAuthorizationState.Authorized) {
      throw BackupProviderException.AuthorizationRequired()
    }
  }

  private fun throwScriptedFailure(operation: Operation) {
    failures[operation]?.pollFirst()?.let { throw it }
  }
}
