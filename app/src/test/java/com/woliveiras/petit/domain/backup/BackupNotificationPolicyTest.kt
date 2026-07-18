package com.woliveiras.petit.domain.backup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.data.repository.BackupFailureCategory
import com.woliveiras.petit.data.repository.BackupSettings
import java.time.Instant
import org.junit.Test

class BackupNotificationPolicyTest {
  @Test
  fun successFollowsPreferenceWhileActionableStatesRemainPermitted() {
    val success = attempt(BackupAttemptStatus.SUCCEEDED)
    assertThat(BackupNotificationPolicy.notificationFor(success, BackupSettings()))
      .isEqualTo(BackupNotificationKind.NONE)
    assertThat(
        BackupNotificationPolicy.notificationFor(success, BackupSettings(notifyAfterSuccess = true))
      )
      .isEqualTo(BackupNotificationKind.SUCCESS)

    val authorizationRequired = attempt(BackupAttemptStatus.AUTHORIZATION_REQUIRED)
    assertThat(BackupNotificationPolicy.notificationFor(authorizationRequired, BackupSettings()))
      .isEqualTo(BackupNotificationKind.AUTHORIZATION_REQUIRED)

    val quotaFailure = attempt(BackupAttemptStatus.FAILED, BackupFailureCategory.QUOTA_EXCEEDED)
    assertThat(BackupNotificationPolicy.notificationFor(quotaFailure, BackupSettings()))
      .isEqualTo(BackupNotificationKind.ACTIONABLE_FAILURE)
    assertThat(
        BackupNotificationPolicy.notificationFor(
          attempt(BackupAttemptStatus.RETRYING, BackupFailureCategory.RETRYABLE),
          BackupSettings(),
        )
      )
      .isEqualTo(BackupNotificationKind.NONE)
  }

  private fun attempt(status: BackupAttemptStatus, failureCategory: BackupFailureCategory? = null) =
    BackupAttempt(
      id = "attempt",
      trigger = BackupTrigger.AUTOMATIC,
      startedAt = Instant.EPOCH,
      completedAt = Instant.EPOCH,
      status = status,
      failureCategory = failureCategory,
    )
}
