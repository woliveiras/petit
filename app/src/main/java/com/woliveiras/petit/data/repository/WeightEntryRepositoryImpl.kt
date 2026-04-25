package com.woliveiras.petit.data.repository

import com.woliveiras.petit.data.local.dao.WeightEntryDao
import com.woliveiras.petit.data.mapper.toDomain
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.domain.model.WeightEntry
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementation of WeightEntryRepository using Room database. */
@Singleton
class WeightEntryRepositoryImpl @Inject constructor(private val weightEntryDao: WeightEntryDao) :
  WeightEntryRepository {

  override fun getWeightEntriesForPet(petId: String): Flow<List<WeightEntry>> {
    return weightEntryDao.getWeightEntriesForPet(petId).map { entities -> entities.toDomain() }
  }

  override suspend fun getLatestWeightEntry(petId: String): WeightEntry? {
    return weightEntryDao.getLatestWeightEntry(petId)?.toDomain()
  }

  override suspend fun getWeightEntryById(id: String): WeightEntry? {
    return weightEntryDao.getWeightEntryById(id)?.toDomain()
  }

  override fun getLatestWeightEntryFlow(petId: String): Flow<WeightEntry?> {
    return weightEntryDao.getLatestWeightEntryFlow(petId).map { entity -> entity?.toDomain() }
  }

  override fun getWeightEntriesForChart(petId: String, limit: Int): Flow<List<WeightEntry>> {
    return weightEntryDao.getWeightEntriesForChart(petId, limit).map { entities ->
      entities.toDomain()
    }
  }

  override fun observeWeightChanges(): Flow<Long?> {
    return weightEntryDao.observeLatestUpdate()
  }

  override suspend fun saveWeightEntry(entry: WeightEntry) {
    val dateMillis = entry.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // Check if there's already an entry for this date (upsert logic)
    val existingEntry = weightEntryDao.getWeightEntryByDate(entry.petId, dateMillis)

    val entryToSave =
      if (existingEntry != null) {
        entry.copy(
          id = existingEntry.id,
          createdAt = existingEntry.createdAt,
          updatedAt = System.currentTimeMillis(),
        )
      } else {
        entry.copy(updatedAt = System.currentTimeMillis())
      }

    val existingById = weightEntryDao.getWeightEntryById(entryToSave.id)
    if (existingById != null) {
      weightEntryDao.updateWeightEntry(entryToSave.toEntity())
    } else {
      weightEntryDao.insertWeightEntry(entryToSave.toEntity())
    }
  }

  override suspend fun deleteWeightEntry(id: String) {
    weightEntryDao.softDeleteWeightEntry(id)
  }

  override suspend fun countEntriesForPet(petId: String): Int {
    return weightEntryDao.countWeightEntriesForPet(petId)
  }
}
