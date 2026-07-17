package com.woliveiras.petit.presentation.feature.weight

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.data.repository.WeightEntryRepository
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.WeightEntry
import com.woliveiras.petit.worker.AutoTaskService
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WeightFormViewModelTest {

  private val dispatcher: TestDispatcher = StandardTestDispatcher()
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val clock = Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC)
  private lateinit var repository: FakeWeightEntryRepository
  private lateinit var viewModel: WeightFormViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    repository = FakeWeightEntryRepository()
    viewModel =
      WeightFormViewModel(
        savedStateHandle = SavedStateHandle(mapOf("petId" to "pet-1")),
        context = context,
        petRepository = FakePetRepository(),
        weightEntryRepository = repository,
        autoTaskService = NoOpAutoTaskService(),
        clock = clock,
      )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun kilogramsAndGramsAreNormalizedBeforePersistence() =
    runTest(dispatcher) {
      viewModel.updateWeight("3.5")
      viewModel.saveWeight()
      advanceUntilIdle()

      viewModel.updateWeightUnit(WeightUnit.GRAMS)
      viewModel.updateWeight("350")
      viewModel.saveWeight()
      advanceUntilIdle()

      assertThat(repository.saved.map { it.weightGrams }).containsExactly(3_500, 350).inOrder()
    }

  @Test
  fun zeroAndValuesAboveFiftyKilogramsAreRejected() =
    runTest(dispatcher) {
      listOf("-1", "0", "50.01").forEach { invalid ->
        viewModel.updateWeightUnit(WeightUnit.KG)
        viewModel.updateWeight(invalid)
        viewModel.saveWeight()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.weightError).isNotNull()
      }

      viewModel.updateWeightUnit(WeightUnit.GRAMS)
      viewModel.updateWeight("50001")
      viewModel.saveWeight()
      advanceUntilIdle()

      assertThat(repository.saved).isEmpty()
      assertThat(viewModel.uiState.value.weightError).isNotNull()
    }

  @Test
  fun futureDateIsRejectedWithoutPersistence() =
    runTest(dispatcher) {
      viewModel.updateWeight("4.2")
      viewModel.updateDate(LocalDate.of(2026, 7, 18))

      viewModel.saveWeight()
      advanceUntilIdle()

      assertThat(repository.saved).isEmpty()
      assertThat(viewModel.uiState.value.dateError).isNotNull()
    }

  @Test
  fun nonFiniteAndMalformedNumericInputsAreRejectedWithoutPersistence() =
    runTest(dispatcher) {
      listOf("NaN", "Infinity", "1e3", "12kg").forEach { invalid ->
        viewModel.updateWeight("")
        viewModel.updateWeight(invalid)
        viewModel.saveWeight()
        advanceUntilIdle()
      }

      assertThat(repository.saved).isEmpty()
      assertThat(viewModel.uiState.value.weightError).isNotNull()
    }

  @Test
  fun editingPreservesIdentityAndCreatedAtAndUsesCurrentUpdatedAt() =
    runTest(dispatcher) {
      val original =
        WeightEntry(
          id = "entry-1",
          petId = "pet-1",
          date = LocalDate.of(2026, 7, 10),
          weightGrams = 4_000,
          createdAt = 10L,
          updatedAt = 20L,
        )
      repository.entries.value = listOf(original)
      advanceUntilIdle()
      viewModel.loadEntryForEdit(original.id)
      advanceUntilIdle()

      viewModel.updateWeight("4.5")
      viewModel.saveWeight()
      advanceUntilIdle()

      val saved = repository.saved.single()
      assertThat(saved.id).isEqualTo(original.id)
      assertThat(saved.weightGrams).isEqualTo(4_500)
      assertThat(saved.createdAt).isEqualTo(10L)
      assertThat(saved.updatedAt).isEqualTo(clock.millis())
    }

  private class FakePetRepository : PetRepository {
    override fun getAllPets(): Flow<List<Pet>> = MutableStateFlow(emptyList())

    override suspend fun getPetById(id: String): Pet? =
      Pet(id = id, name = "Mimi", createdAt = 1L, updatedAt = 1L)

    override fun getPetByIdFlow(id: String): Flow<Pet?> = MutableStateFlow(null)

    override fun getPetCount(): Flow<Int> = MutableStateFlow(0)

    override suspend fun savePet(pet: Pet) = Unit

    override suspend fun deletePet(id: String) = Unit
  }

  private class FakeWeightEntryRepository : WeightEntryRepository {
    val entries = MutableStateFlow<List<WeightEntry>>(emptyList())
    val saved = mutableListOf<WeightEntry>()

    override fun getWeightEntriesForPet(petId: String): Flow<List<WeightEntry>> = entries

    override suspend fun getLatestWeightEntry(petId: String): WeightEntry? =
      entries.value.firstOrNull()

    override suspend fun getWeightEntryById(id: String): WeightEntry? =
      entries.value.find { it.id == id }

    override fun getLatestWeightEntryFlow(petId: String): Flow<WeightEntry?> =
      MutableStateFlow(entries.value.firstOrNull())

    override fun getWeightEntriesForChart(petId: String, limit: Int): Flow<List<WeightEntry>> =
      entries

    override fun observeWeightChanges(): Flow<Long?> = MutableStateFlow(null)

    override suspend fun saveWeightEntry(entry: WeightEntry) {
      saved += entry
    }

    override suspend fun deleteWeightEntry(id: String) = Unit

    override suspend fun countEntriesForPet(petId: String): Int = entries.value.size
  }

  private class NoOpAutoTaskService : AutoTaskService {
    override suspend fun handleVaccinationSaved(entry: VaccinationEntry) = Unit

    override suspend fun handleVaccinationDeleted(entryId: String) = Unit

    override suspend fun handleDewormingSaved(entry: DewormingEntry) = Unit

    override suspend fun handleDewormingDeleted(entryId: String) = Unit

    override suspend fun handleWeightSaved(petId: String, petName: String) = Unit

    override suspend fun cancelWeightTask(petId: String) = Unit
  }
}
