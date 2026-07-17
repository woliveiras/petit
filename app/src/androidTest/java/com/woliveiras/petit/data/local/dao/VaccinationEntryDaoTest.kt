package com.woliveiras.petit.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.local.entity.VaccinationEntryEntity
import com.woliveiras.petit.data.repository.VaccinationEntryRepositoryImpl
import com.woliveiras.petit.domain.model.SyncStatus
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaccinationEntryDaoTest {

  private lateinit var database: PetitDatabase
  private lateinit var dao: VaccinationEntryDao

  @Before
  fun setUp() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java).build()
    dao = database.vaccinationEntryDao()
    database.petDao().insertPet(PetEntity(id = "pet-1", name = "Mimi", petType = "CAT"))
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun latestSelectionIsDeterministicAndCompleteHistoryIsPreserved() = runTest {
    dao.insertVaccinationEntry(entry("old", applicationDate = 100L, updatedAt = 100L))
    dao.insertVaccinationEntry(entry("older-update", applicationDate = 200L, updatedAt = 200L))
    dao.insertVaccinationEntry(entry("tie-a", applicationDate = 200L, updatedAt = 300L))
    dao.insertVaccinationEntry(entry("tie-b", applicationDate = 200L, updatedAt = 300L))

    assertThat(dao.getLatestVaccinationsForPet("pet-1").first().map { it.id })
      .containsExactly("tie-b")
    assertThat(dao.getVaccinationEntriesForPet("pet-1").first().map { it.id })
      .containsExactly("tie-b", "tie-a", "older-update", "old")
      .inOrder()
  }

  @Test
  fun updatePersistsTraceabilityAndSoftDeleteHidesOnlyDeletedDose() = runTest {
    dao.insertVaccinationEntry(entry("entry-1", applicationDate = 100L, updatedAt = 100L))
    dao.insertVaccinationEntry(
      entry("survivor", applicationDate = 50L, updatedAt = 50L).copy(vaccineType = "RABIES")
    )
    val updated =
      entry("entry-1", applicationDate = 100L, updatedAt = 200L)
        .copy(
          customVaccineTypeName = "Especial",
          veterinarian = "Dra. Ana",
          clinic = "Petit Vet",
          batchNumber = "lote-7",
          note = "reforço",
        )

    dao.updateVaccinationEntry(updated)

    assertThat(dao.getVaccinationEntryById("entry-1")).isEqualTo(updated)

    dao.softDeleteVaccinationEntry("entry-1", timestamp = 300L)

    assertThat(dao.getVaccinationEntryById("entry-1")).isNull()
    assertThat(dao.getVaccinationEntriesForPet("pet-1").first().map { it.id })
      .containsExactly("survivor")
  }

  @Test
  fun repositoryPreservesTimestampProvidedByControlledClock() = runTest {
    val repository =
      VaccinationEntryRepositoryImpl(
        dao,
        Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC),
      )
    val controlledTimestamp = 123_456L
    val entry =
      VaccinationEntry(
        id = "entry-clock",
        petId = "pet-1",
        vaccineType = VaccineType.RABIES,
        applicationDate = LocalDate.of(2026, 7, 1),
        createdAt = controlledTimestamp,
        updatedAt = controlledTimestamp,
        syncStatus = SyncStatus.LOCAL_ONLY,
      )

    repository.saveVaccinationEntry(entry)

    assertThat(dao.getVaccinationEntryById(entry.id)?.updatedAt).isEqualTo(controlledTimestamp)
  }

  private fun entry(id: String, applicationDate: Long, updatedAt: Long) =
    VaccinationEntryEntity(
      id = id,
      petId = "pet-1",
      vaccineType = "OTHER",
      applicationDate = applicationDate,
      createdAt = 10L,
      updatedAt = updatedAt,
    )
}
