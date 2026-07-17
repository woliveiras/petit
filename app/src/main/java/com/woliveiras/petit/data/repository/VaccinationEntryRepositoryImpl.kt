package com.woliveiras.petit.data.repository

import com.woliveiras.petit.data.local.dao.VaccinationEntryDao
import com.woliveiras.petit.data.mapper.toDomain
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.domain.model.VaccinationEntry
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementation of VaccinationEntryRepository using Room database. */
@Singleton
class VaccinationEntryRepositoryImpl
@Inject
constructor(private val vaccinationEntryDao: VaccinationEntryDao, private val clock: Clock) :
  VaccinationEntryRepository {

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
    val today = LocalDate.now(clock).atStartOfDay(clock.zone).toInstant().toEpochMilli()
    return vaccinationEntryDao.getOverdueVaccinations(today).map { entities -> entities.toDomain() }
  }

  override fun getUpcomingVaccinations(days: Int): Flow<List<VaccinationEntry>> {
    val today = LocalDate.now(clock).atStartOfDay(clock.zone).toInstant().toEpochMilli()
    val futureDate =
      LocalDate.now(clock)
        .plusDays(days.toLong())
        .atStartOfDay(clock.zone)
        .toInstant()
        .toEpochMilli()
    return vaccinationEntryDao.getUpcomingVaccinations(today, futureDate).map { entities ->
      entities.toDomain()
    }
  }

  override suspend fun saveVaccinationEntry(entry: VaccinationEntry) {
    val existingEntry = vaccinationEntryDao.getVaccinationEntryById(entry.id)
    if (existingEntry != null) {
      vaccinationEntryDao.updateVaccinationEntry(entry.toEntity())
    } else {
      vaccinationEntryDao.insertVaccinationEntry(entry.toEntity())
    }
  }

  override suspend fun deleteVaccinationEntry(id: String) {
    vaccinationEntryDao.softDeleteVaccinationEntry(id)
  }

  override suspend fun countEntriesForPet(petId: String): Int {
    return vaccinationEntryDao.countVaccinationEntriesForPet(petId)
  }
}
