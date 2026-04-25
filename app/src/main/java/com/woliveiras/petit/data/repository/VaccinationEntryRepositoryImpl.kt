package com.woliveiras.petit.data.repository

import com.woliveiras.petit.data.local.dao.VaccinationEntryDao
import com.woliveiras.petit.data.mapper.toDomain
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.domain.model.VaccinationEntry
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementation of VaccinationEntryRepository using Room database. */
@Singleton
class VaccinationEntryRepositoryImpl
@Inject
constructor(private val vaccinationEntryDao: VaccinationEntryDao) : VaccinationEntryRepository {

  override fun getVaccinationEntriesForPet(petId: String): Flow<List<VaccinationEntry>> {
    return vaccinationEntryDao.getVaccinationEntriesForPet(petId).map { entities ->
      entities.toDomain()
    }
  }

  override fun getLatestVaccinationsForPet(petId: String): Flow<List<VaccinationEntry>> {
    return vaccinationEntryDao.getLatestVaccinationsForPet(petId).map { entities ->
      entities.toDomain()
    }
  }

  override suspend fun getVaccinationEntryById(id: String): VaccinationEntry? {
    return vaccinationEntryDao.getVaccinationEntryById(id)?.toDomain()
  }

  override fun getOverdueVaccinations(): Flow<List<VaccinationEntry>> {
    val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return vaccinationEntryDao.getOverdueVaccinations(today).map { entities -> entities.toDomain() }
  }

  override fun getUpcomingVaccinations(days: Int): Flow<List<VaccinationEntry>> {
    val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val futureDate =
      LocalDate.now()
        .plusDays(days.toLong())
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    return vaccinationEntryDao.getUpcomingVaccinations(today, futureDate).map { entities ->
      entities.toDomain()
    }
  }

  override suspend fun saveVaccinationEntry(entry: VaccinationEntry) {
    val updatedEntry = entry.copy(updatedAt = System.currentTimeMillis())
    val existingEntry = vaccinationEntryDao.getVaccinationEntryById(entry.id)
    if (existingEntry != null) {
      vaccinationEntryDao.updateVaccinationEntry(updatedEntry.toEntity())
    } else {
      vaccinationEntryDao.insertVaccinationEntry(updatedEntry.toEntity())
    }
  }

  override suspend fun deleteVaccinationEntry(id: String) {
    vaccinationEntryDao.softDeleteVaccinationEntry(id)
  }

  override suspend fun countEntriesForPet(petId: String): Int {
    return vaccinationEntryDao.countVaccinationEntriesForPet(petId)
  }
}
