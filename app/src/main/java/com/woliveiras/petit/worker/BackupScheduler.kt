package com.woliveiras.petit.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.woliveiras.petit.domain.backup.BackupNetworkRequirement
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface BackupScheduler {
  fun schedulePeriodic(networkRequirement: BackupNetworkRequirement)

  fun cancelPeriodic()
}

@Singleton
class WorkManagerBackupScheduler internal constructor(private val workManager: WorkManager) :
  BackupScheduler {
  @Inject constructor(@ApplicationContext context: Context) : this(WorkManager.getInstance(context))

  override fun schedulePeriodic(networkRequirement: BackupNetworkRequirement) {
    workManager.enqueueUniquePeriodicWork(
      PERIODIC_WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      periodicRequest(networkRequirement),
    )
  }

  override fun cancelPeriodic() {
    workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
  }

  companion object {
    const val PERIODIC_WORK_NAME = "petit_periodic_backup"
    const val PERIODIC_WORK_TAG = "petit_backup"
    const val REPEAT_INTERVAL_HOURS = 24L
    const val BACKOFF_SECONDS = 30L

    internal fun periodicRequest(
      networkRequirement: BackupNetworkRequirement
    ): PeriodicWorkRequest =
      PeriodicWorkRequestBuilder<AutomaticBackupWorker>(REPEAT_INTERVAL_HOURS, TimeUnit.HOURS)
        .setConstraints(
          Constraints.Builder()
            .setRequiredNetworkType(
              when (networkRequirement) {
                BackupNetworkRequirement.CONNECTED -> NetworkType.CONNECTED
                BackupNetworkRequirement.UNMETERED -> NetworkType.UNMETERED
              }
            )
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
        )
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(BACKOFF_SECONDS))
        .addTag(PERIODIC_WORK_TAG)
        .build()
  }
}
