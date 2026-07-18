package com.woliveiras.petit.data.repository

import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupTrigger
import java.time.Instant
import kotlinx.coroutines.flow.Flow

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

interface BackupAttemptRepository {
  val attempts: Flow<List<BackupAttempt>>

  suspend fun getAttempt(id: String): BackupAttempt?

  suspend fun upsert(attempt: BackupAttempt)
}
