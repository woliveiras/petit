package com.woliveiras.petit.domain.backup

import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.data.repository.BackupFailureCategory
import com.woliveiras.petit.data.repository.BackupSettings

enum class BackupNotificationKind {
  NONE,
  SUCCESS,
  AUTHORIZATION_REQUIRED,
  ACTIONABLE_FAILURE,
}

object BackupNotificationPolicy {
  fun notificationFor(attempt: BackupAttempt, settings: BackupSettings): BackupNotificationKind =
    when {
      attempt.status == BackupAttemptStatus.SUCCEEDED && settings.notifyAfterSuccess ->
        BackupNotificationKind.SUCCESS
      attempt.status == BackupAttemptStatus.AUTHORIZATION_REQUIRED ->
        BackupNotificationKind.AUTHORIZATION_REQUIRED
      attempt.status == BackupAttemptStatus.FAILED &&
        attempt.failureCategory in
          setOf(BackupFailureCategory.QUOTA_EXCEEDED, BackupFailureCategory.PERMANENT) ->
        BackupNotificationKind.ACTIONABLE_FAILURE
      else -> BackupNotificationKind.NONE
    }
}
