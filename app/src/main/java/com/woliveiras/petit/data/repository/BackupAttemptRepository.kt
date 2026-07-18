package com.woliveiras.petit.data.repository

import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupTrigger
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

enum class BackupAttemptStatus {
  RUNNING,
  SUCCEEDED,
  RETRYING,
  FAILED,
  CANCELLED,
  AUTHORIZATION_REQUIRED,
}

enum class BackupFailureCategory {
  AUTHORIZATION_REQUIRED,
  QUOTA_EXCEEDED,
  RETRYABLE,
  PERMANENT,
}

data class BackupAttempt(
  val id: String,
  val trigger: BackupTrigger,
  val startedAt: Instant,
  val completedAt: Instant? = null,
  val status: BackupAttemptStatus,
  val archiveSizeBytes: Long? = null,
  val contentCounts: BackupContentCounts? = null,
  val failureCategory: BackupFailureCategory? = null,
) {
  init {
    require(id.isNotBlank()) { "Attempt ID cannot be blank" }
    require('|' !in id) { "Attempt ID contains a reserved character" }
    require(archiveSizeBytes == null || archiveSizeBytes >= 0) { "Archive size cannot be negative" }
  }
}

data class BackupAttemptCursor(val startedAt: Instant, val id: String)

data class BackupAttemptPage(val items: List<BackupAttempt>, val nextCursor: BackupAttemptCursor?)

interface BackupAttemptRepository {
  val attempts: Flow<List<BackupAttempt>>

  suspend fun getPage(after: BackupAttemptCursor? = null, limit: Int): BackupAttemptPage {
    require(limit in 1..MAX_HISTORY_PAGE_SIZE) { "History page size is out of range" }
    val ordered = attempts.first().sortedWith(BACKUP_ATTEMPT_ORDER)
    val remaining =
      if (after == null) {
        ordered
      } else {
        ordered.dropWhile {
          it.startedAt > after.startedAt || (it.startedAt == after.startedAt && it.id <= after.id)
        }
      }
    val items = remaining.take(limit)
    val nextCursor =
      items
        .lastOrNull()
        ?.takeIf { remaining.size > items.size }
        ?.let { BackupAttemptCursor(startedAt = it.startedAt, id = it.id) }
    return BackupAttemptPage(items = items, nextCursor = nextCursor)
  }

  suspend fun getAttempt(id: String): BackupAttempt?

  suspend fun upsert(attempt: BackupAttempt)
}

internal val BACKUP_ATTEMPT_ORDER =
  compareByDescending<BackupAttempt> { it.startedAt }.thenBy { it.id }

private const val MAX_HISTORY_PAGE_SIZE = 100
