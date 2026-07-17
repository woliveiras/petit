package com.woliveiras.petit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.domain.model.WeightEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeightEntryRepositoryIntegrationTest {

  private lateinit var database: PetitDatabase
  private lateinit var repository: WeightEntryRepositoryImpl

  @Before
  fun setUp() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java).build()
    repository = WeightEntryRepositoryImpl(database.weightEntryDao())
    database.petDao().insertPet(PetEntity(id = "pet-1", name = "Mimi"))
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun savingTheSamePetAndDayReplacesTheActiveEntry() = runTest {
    repository.saveWeightEntry(entry("first", day = 10, grams = 4_000, createdAt = 10L))
    repository.saveWeightEntry(entry("replacement", day = 10, grams = 4_500, createdAt = 20L))

    val active = repository.getWeightEntriesForPet("pet-1").first()

    assertThat(active).hasSize(1)
    assertThat(active.single().id).isEqualTo("first")
    assertThat(active.single().weightGrams).isEqualTo(4_500)
    assertThat(active.single().createdAt).isEqualTo(10L)
  }

  @Test
  fun historyIsDescendingAndChartLimitReturnsTheLatestEntriesAscending() = runTest {
    repository.saveWeightEntry(entry("oldest", day = 1, grams = 4_000))
    repository.saveWeightEntry(entry("middle", day = 10, grams = 4_200))
    repository.saveWeightEntry(entry("latest", day = 20, grams = 4_400))

    val history = repository.getWeightEntriesForPet("pet-1").first()
    val chart = repository.getWeightEntriesForChart("pet-1", limit = 2).first()

    assertThat(history.map { it.id }).containsExactly("latest", "middle", "oldest").inOrder()
    assertThat(chart.map { it.id }).containsExactly("middle", "latest").inOrder()
  }

  @Test
  fun deletionIsSoftAndAllActiveQueriesExcludeTheEntry() = runTest {
    repository.saveWeightEntry(entry("entry-1", day = 10, grams = 4_000))

    repository.deleteWeightEntry("entry-1")

    assertThat(repository.getWeightEntriesForPet("pet-1").first()).isEmpty()
    assertThat(repository.getWeightEntriesForChart("pet-1").first()).isEmpty()
    assertThat(repository.getWeightEntryById("entry-1")).isNull()
    assertThat(repository.countEntriesForPet("pet-1")).isEqualTo(0)
    database.openHelper.readableDatabase
      .query("SELECT deletedAt FROM weight_entries WHERE id = 'entry-1'")
      .use { cursor ->
        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.isNull(0)).isFalse()
      }
  }

  @Test
  fun movingAnEditOntoAnOccupiedDayReplacesThatDayAndRemovesTheOldActiveDate() = runTest {
    val edited = entry("edited", day = 10, grams = 4_000, createdAt = 10L)
    repository.saveWeightEntry(edited)
    repository.saveWeightEntry(entry("occupied", day = 20, grams = 4_200, createdAt = 20L))

    repository.saveWeightEntry(edited.copy(date = LocalDate.of(2026, 7, 20), weightGrams = 4_500))

    val active = repository.getWeightEntriesForPet("pet-1").first()
    assertThat(active).hasSize(1)
    assertThat(active.single().id).isEqualTo("edited")
    assertThat(active.single().createdAt).isEqualTo(10L)
    assertThat(active.single().date).isEqualTo(LocalDate.of(2026, 7, 20))
    assertThat(active.single().weightGrams).isEqualTo(4_500)
    database.openHelper.readableDatabase
      .query("SELECT deletedAt FROM weight_entries WHERE id = 'occupied'")
      .use { cursor ->
        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.isNull(0)).isFalse()
      }
  }

  private fun entry(id: String, day: Int, grams: Int, createdAt: Long = day.toLong()) =
    WeightEntry(
      id = id,
      petId = "pet-1",
      date = LocalDate.of(2026, 7, day),
      weightGrams = grams,
      createdAt = createdAt,
      updatedAt = createdAt,
    )
}
