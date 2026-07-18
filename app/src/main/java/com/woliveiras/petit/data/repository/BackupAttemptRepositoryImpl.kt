package com.woliveiras.petit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupTrigger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.backupAttemptDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "backup_attempt_history")

@Singleton
class BackupAttemptRepositoryImpl
internal constructor(private val dataStore: DataStore<Preferences>) : BackupAttemptRepository {
  @Inject constructor(@ApplicationContext context: Context) : this(context.backupAttemptDataStore)

  override val attempts: Flow<List<BackupAttempt>> =
    dataStore.data.map { preferences ->
      preferences[ATTEMPTS].orEmpty().mapNotNull(::decode).sortedByDescending { it.startedAt }
    }

  override suspend fun getAttempt(id: String): BackupAttempt? =
    attempts.first().firstOrNull { it.id == id }

  override suspend fun upsert(attempt: BackupAttempt) {
    dataStore.edit { preferences ->
      val retained = preferences[ATTEMPTS].orEmpty().filterNot { encodedId(it) == attempt.id }
      preferences[ATTEMPTS] = retained.toSet() + encode(attempt)
    }
  }

  private fun encode(attempt: BackupAttempt): String {
    val counts = attempt.contentCounts
    return listOf(
        attempt.id,
        attempt.trigger.name,
        attempt.startedAt.toEpochMilli().toString(),
        attempt.completedAt?.toEpochMilli()?.toString().orEmpty(),
        attempt.status.name,
        attempt.archiveSizeBytes?.toString().orEmpty(),
        counts?.pets?.toString().orEmpty(),
        counts?.weights?.toString().orEmpty(),
        counts?.vaccinations?.toString().orEmpty(),
        counts?.dewormingRecords?.toString().orEmpty(),
        counts?.tasks?.toString().orEmpty(),
        counts?.assets?.toString().orEmpty(),
        attempt.failureCategory?.name.orEmpty(),
      )
      .joinToString(SEPARATOR)
  }

  private fun decode(encoded: String): BackupAttempt? =
    runCatching {
        val values = encoded.split(SEPARATOR)
        require(values.size == FIELD_COUNT)
        val hasCounts = values.subList(6, 12).any(String::isNotEmpty)
        BackupAttempt(
          id = values[0],
          trigger = BackupTrigger.valueOf(values[1]),
          startedAt = Instant.ofEpochMilli(values[2].toLong()),
          completedAt = values[3].toLongOrNull()?.let(Instant::ofEpochMilli),
          status = BackupAttemptStatus.valueOf(values[4]),
          archiveSizeBytes = values[5].toLongOrNull(),
          contentCounts =
            if (hasCounts) {
              BackupContentCounts(
                pets = values[6].toInt(),
                weights = values[7].toInt(),
                vaccinations = values[8].toInt(),
                dewormingRecords = values[9].toInt(),
                tasks = values[10].toInt(),
                assets = values[11].toInt(),
              )
            } else {
              null
            },
          failureCategory =
            values[12].takeIf(String::isNotEmpty)?.let(BackupFailureCategory::valueOf),
        )
      }
      .getOrNull()

  private fun encodedId(encoded: String): String = encoded.substringBefore(SEPARATOR)

  private companion object {
    val ATTEMPTS = stringSetPreferencesKey("backup_attempts")
    const val SEPARATOR = "|"
    const val FIELD_COUNT = 13
  }
}
