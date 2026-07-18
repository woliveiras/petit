package com.woliveiras.petit.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.woliveiras.petit.MainActivity
import com.woliveiras.petit.R
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupFailureCategory
import com.woliveiras.petit.data.repository.BackupSettingsRepository
import com.woliveiras.petit.domain.backup.BackupNotificationKind
import com.woliveiras.petit.domain.backup.BackupNotificationPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

fun interface BackupNotificationDispatcher {
  suspend fun dispatch(attempt: BackupAttempt)
}

@Singleton
class AndroidBackupNotificationDispatcher
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val settingsRepository: BackupSettingsRepository,
) : BackupNotificationDispatcher {
  override suspend fun dispatch(attempt: BackupAttempt) {
    val kind = BackupNotificationPolicy.notificationFor(attempt, settingsRepository.getSettings())
    if (kind == BackupNotificationKind.NONE || !notificationsAllowed()) return

    createSilentChannel()
    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        NOTIFICATION_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val text =
      when (kind) {
        BackupNotificationKind.SUCCESS ->
          context.getString(
            R.string.backup_notification_success,
            attempt.contentCounts?.pets ?: 0,
            attempt.contentCounts?.tasks ?: 0,
            attempt.archiveSizeBytes ?: 0L,
          )
        BackupNotificationKind.AUTHORIZATION_REQUIRED ->
          context.getString(R.string.backup_authorization_required)
        BackupNotificationKind.ACTIONABLE_FAILURE ->
          if (attempt.failureCategory == BackupFailureCategory.QUOTA_EXCEEDED) {
            context.getString(R.string.backup_quota_exceeded)
          } else {
            context.getString(R.string.backup_permanent_error)
          }
        BackupNotificationKind.NONE -> return
      }
    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.backup_settings_title))
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setSilent(true)
        .setOnlyAlertOnce(true)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(NOTIFICATION_ID, notification)
  }

  private fun notificationsAllowed(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
      PackageManager.PERMISSION_GRANTED

  private fun createSilentChannel() {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel =
      NotificationChannel(
          CHANNEL_ID,
          context.getString(R.string.backup_settings_title),
          NotificationManager.IMPORTANCE_LOW,
        )
        .apply {
          description = context.getString(R.string.backup_ready)
          enableVibration(false)
          setSound(null, null)
        }
    manager.createNotificationChannel(channel)
  }

  companion object {
    const val CHANNEL_ID = "petit_backup"
    const val NOTIFICATION_ID = 3_061
    private const val NOTIFICATION_REQUEST_CODE = 3_061
  }
}

object NoOpBackupNotificationDispatcher : BackupNotificationDispatcher {
  override suspend fun dispatch(attempt: BackupAttempt) = Unit
}
