package com.woliveiras.petit.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupTrigger
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupAttemptRepositoryTest {
  @Test
  fun historyPagesAreBoundedStableAndDoNotRepeatAttempts() = runTest {
    val file = File.createTempFile("backup-attempt-pages-", ".preferences_pb").also { it.delete() }
    val job = SupervisorJob()
    val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + job)
    try {
      val repository =
        BackupAttemptRepositoryImpl(
          PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        )
      val newest = Instant.parse("2026-07-18T09:00:00Z")
      (1..12).forEach { index ->
        repository.upsert(
          BackupAttempt(
            id = "attempt-${index.toString().padStart(2, '0')}",
            trigger = BackupTrigger.AUTOMATIC,
            startedAt = if (index <= 2) newest else newest.minusSeconds(index.toLong()),
            status = BackupAttemptStatus.SUCCEEDED,
          )
        )
      }

      val first = repository.getPage(limit = 5)
      val second = repository.getPage(after = first.nextCursor, limit = 5)
      val last = repository.getPage(after = second.nextCursor, limit = 5)

      assertThat(first.items.map { it.id })
        .containsExactly("attempt-01", "attempt-02", "attempt-03", "attempt-04", "attempt-05")
        .inOrder()
      assertThat(second.items.map { it.id })
        .containsExactly("attempt-06", "attempt-07", "attempt-08", "attempt-09", "attempt-10")
        .inOrder()
      assertThat(last.items.map { it.id }).containsExactly("attempt-11", "attempt-12").inOrder()
      assertThat((first.items + second.items + last.items).map { it.id }).containsNoDuplicates()
      assertThat(first.nextCursor).isNotNull()
      assertThat(second.nextCursor).isNotNull()
      assertThat(last.nextCursor).isNull()
    } finally {
      job.cancelAndJoin()
      scope.cancel()
      file.delete()
    }
  }

  @Test
  fun upsertedNonClinicalHistoryPersistsAcrossRepositoryInstances() = runTest {
    val file = File.createTempFile("backup-attempts-", ".preferences_pb").also { it.delete() }
    val firstJob = SupervisorJob()
    val firstScope = CoroutineScope(StandardTestDispatcher(testScheduler) + firstJob)
    try {
      val repository =
        BackupAttemptRepositoryImpl(
          PreferenceDataStoreFactory.create(scope = firstScope, produceFile = { file })
        )
      val startedAt = Instant.parse("2026-07-18T08:00:00Z")
      repository.upsert(
        BackupAttempt(
          id = "attempt-1",
          trigger = BackupTrigger.AUTOMATIC,
          startedAt = startedAt,
          status = BackupAttemptStatus.RUNNING,
        )
      )
      repository.upsert(
        BackupAttempt(
          id = "attempt-1",
          trigger = BackupTrigger.AUTOMATIC,
          startedAt = startedAt,
          completedAt = startedAt.plusSeconds(2),
          status = BackupAttemptStatus.SUCCEEDED,
          archiveSizeBytes = 512,
          contentCounts = BackupContentCounts(pets = 1, tasks = 3),
        )
      )

      firstJob.cancelAndJoin()
      advanceUntilIdle()
      val reopenedJob = SupervisorJob()
      val reopenedScope = CoroutineScope(StandardTestDispatcher(testScheduler) + reopenedJob)
      val restored =
        BackupAttemptRepositoryImpl(
            PreferenceDataStoreFactory.create(scope = reopenedScope, produceFile = { file })
          )
          .attempts
          .first()

      assertThat(restored).hasSize(1)
      assertThat(restored.single().status).isEqualTo(BackupAttemptStatus.SUCCEEDED)
      assertThat(restored.single().archiveSizeBytes).isEqualTo(512L)
      assertThat(restored.single().contentCounts)
        .isEqualTo(BackupContentCounts(pets = 1, tasks = 3))
      reopenedJob.cancelAndJoin()
      reopenedScope.cancel()
    } finally {
      firstScope.cancel()
      file.delete()
    }
  }
}
