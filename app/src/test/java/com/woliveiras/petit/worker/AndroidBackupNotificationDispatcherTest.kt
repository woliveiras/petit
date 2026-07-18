package com.woliveiras.petit.worker

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.data.repository.BackupFailureCategory
import com.woliveiras.petit.data.repository.BackupSettings
import com.woliveiras.petit.data.repository.BackupSettingsRepository
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupTrigger
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidBackupNotificationDispatcherTest {
  private val context = ApplicationProvider.getApplicationContext<Application>()
  private val manager by lazy {
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  @Before
  fun setUp() {
    shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    manager.cancelAll()
  }

  @After
  fun tearDown() {
    manager.cancelAll()
    manager.deleteNotificationChannel(AndroidBackupNotificationDispatcher.CHANNEL_ID)
  }

  @Test
  fun successIsSilentLocalizedAndContainsNoClinicalData() = runTest {
    val settings = FakeSettingsRepository(BackupSettings(notifyAfterSuccess = true))
    val dispatcher = AndroidBackupNotificationDispatcher(context, settings)

    dispatcher.dispatch(
      attempt(
        status = BackupAttemptStatus.SUCCEEDED,
        archiveSizeBytes = 42,
        contentCounts = BackupContentCounts(pets = 2, tasks = 3),
      )
    )

    val notification = manager.activeNotifications.single().notification
    assertThat(notification.extras.getCharSequence("android.title").toString())
      .isEqualTo(context.getString(R.string.backup_settings_title))
    assertThat(notification.extras.getCharSequence("android.text").toString())
      .isEqualTo(context.getString(R.string.backup_notification_success, 2, 3, 42L))
    assertThat(notification.extras.toString()).doesNotContain("Mimi")
    assertThat(notification.extras.toString()).doesNotContain("rabies")
    assertThat(notification.contentIntent).isNotNull()
    val channel = manager.getNotificationChannel(AndroidBackupNotificationDispatcher.CHANNEL_ID)
    assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_LOW)
    assertThat(channel.sound).isNull()
    assertThat(channel.shouldVibrate()).isFalse()
  }

  @Test
  fun successPreferenceAndPermissionSuppressDeliveryButAuthorizationRemainsEligible() = runTest {
    val settings = FakeSettingsRepository(BackupSettings(notifyAfterSuccess = false))
    val dispatcher = AndroidBackupNotificationDispatcher(context, settings)

    dispatcher.dispatch(attempt(BackupAttemptStatus.SUCCEEDED, archiveSizeBytes = 42))
    assertThat(manager.activeNotifications).isEmpty()

    dispatcher.dispatch(attempt(BackupAttemptStatus.AUTHORIZATION_REQUIRED))
    assertThat(manager.activeNotifications).hasLength(1)
    assertThat(manager.activeNotifications.single().notification.extras.toString())
      .contains(context.getString(R.string.backup_authorization_required))

    manager.cancelAll()
    shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
    dispatcher.dispatch(
      attempt(BackupAttemptStatus.FAILED, failureCategory = BackupFailureCategory.QUOTA_EXCEEDED)
    )
    assertThat(manager.activeNotifications).isEmpty()
  }

  private fun attempt(
    status: BackupAttemptStatus,
    failureCategory: BackupFailureCategory? = null,
    archiveSizeBytes: Long? = null,
    contentCounts: BackupContentCounts? = null,
  ) =
    BackupAttempt(
      id = "attempt-1",
      trigger = BackupTrigger.AUTOMATIC,
      startedAt = Instant.EPOCH,
      completedAt = Instant.EPOCH,
      status = status,
      archiveSizeBytes = archiveSizeBytes,
      contentCounts = contentCounts,
      failureCategory = failureCategory,
    )

  private class FakeSettingsRepository(initial: BackupSettings) : BackupSettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<BackupSettings> = state

    override suspend fun getSettings(): BackupSettings = state.value

    override suspend fun updateAutomaticBackupEnabled(enabled: Boolean) = Unit

    override suspend fun updateNetworkRequirement(
      requirement: com.woliveiras.petit.domain.backup.BackupNetworkRequirement
    ) = Unit

    override suspend fun updateNotifyAfterSuccess(enabled: Boolean) = Unit
  }
}
