package com.woliveiras.petit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PetitApplication : Application(), Configuration.Provider {

  @Inject lateinit var workerFactory: HiltWorkerFactory

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

  private fun createNotificationChannel() {
    val channel =
      NotificationChannel(
          TASKS_CHANNEL_ID,
          getString(R.string.notification_channel_reminders),
          NotificationManager.IMPORTANCE_HIGH,
        )
        .apply {
          description = getString(R.string.notification_channel_reminders_description)
          enableVibration(true)
        }

    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
  }

  companion object {
    const val TASKS_CHANNEL_ID = "petit_reminders"
  }
}
