package com.woliveiras.petit.presentation.feature.deworming

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.DewormingEntryRepository
import com.woliveiras.petit.data.repository.PetRepository
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.worker.AutoTaskService
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DewormingViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val petRepository = FakePetRepository()
  private val dewormingRepository = FakeDewormingEntryRepository()
  private val autoTaskService = NoOpAutoTaskService()

  private lateinit var viewModel: DewormingViewModel

  @Before
  fun setUp() {
    viewModel =
      DewormingViewModel(
        savedStateHandle = SavedStateHandle(mapOf("petId" to "pet-1")),
        context = context,
        petRepository = petRepository,
        dewormingRepository = dewormingRepository,
        autoTaskService = autoTaskService,
      )
  }

  @Test
  fun updateApplicationDateKeepsSelectedIntervalOffset() = runTest {
    val initialApplicationDate = LocalDate.of(2026, 7, 1)
    val newApplicationDate = LocalDate.of(2026, 7, 10)

    viewModel.updateApplicationDate(initialApplicationDate)
    viewModel.updateNextDueDate(initialApplicationDate.plusMonths(2))
    viewModel.updateApplicationDate(newApplicationDate)

    assertThat(viewModel.uiState.value.form.nextDueDate).isEqualTo(newApplicationDate.plusMonths(2))
  }

  @Test
  fun selectCustomIntervalClearsStaleNextDueDate() = runTest {
    val applicationDate = LocalDate.of(2026, 7, 1)

    viewModel.updateApplicationDate(applicationDate)
    viewModel.updateMonthlyInterval(2)
    viewModel.selectCustomInterval()

    assertThat(viewModel.uiState.value.form.customIntervalValue).isEmpty()
    assertThat(viewModel.uiState.value.form.nextDueDate).isNull()
  }

  @Test
  fun updateApplicationDateRecalculatesCustomInterval() = runTest {
    val initialApplicationDate = LocalDate.of(2026, 7, 1)
    val newApplicationDate = LocalDate.of(2026, 7, 10)

    viewModel.updateApplicationDate(initialApplicationDate)
    viewModel.selectCustomInterval()
    viewModel.updateCustomIntervalUnit(DewormingIntervalUnit.DAILY)
    viewModel.updateCustomIntervalValue("15")
    viewModel.updateApplicationDate(newApplicationDate)

    assertThat(viewModel.uiState.value.form.nextDueDate).isEqualTo(newApplicationDate.plusDays(15))
  }

  private class FakePetRepository : PetRepository {
    override fun getAllPets(): Flow<List<Pet>> = MutableStateFlow(emptyList())

    override suspend fun getPetById(id: String): Pet? = null

    override fun getPetByIdFlow(id: String): Flow<Pet?> = MutableStateFlow(null)

    override fun getPetCount(): Flow<Int> = MutableStateFlow(0)

    override suspend fun savePet(pet: Pet) = Unit

    override suspend fun deletePet(id: String) = Unit
  }

  private class FakeDewormingEntryRepository : DewormingEntryRepository {
    override fun getDewormingEntriesForPet(petId: String): Flow<List<DewormingEntry>> =
      MutableStateFlow(emptyList())

    override fun getLatestDewormingsForPet(petId: String): Flow<List<DewormingEntry>> =
      MutableStateFlow(emptyList())

    override suspend fun getDewormingEntryById(id: String): DewormingEntry? = null

    override fun getOverdueDewormings(): Flow<List<DewormingEntry>> = MutableStateFlow(emptyList())

    override fun getUpcomingDewormings(days: Int): Flow<List<DewormingEntry>> =
      MutableStateFlow(emptyList())

    override suspend fun saveDewormingEntry(entry: DewormingEntry) = Unit

    override suspend fun deleteDewormingEntry(id: String) = Unit

    override suspend fun countEntriesForPet(petId: String): Int = 0
  }

  private class NoOpAutoTaskService : AutoTaskService {
    override suspend fun handleVaccinationSaved(
      entry: com.woliveiras.petit.domain.model.VaccinationEntry
    ) = Unit

    override suspend fun handleVaccinationDeleted(entryId: String) = Unit

    override suspend fun handleDewormingSaved(entry: DewormingEntry) = Unit

    override suspend fun handleDewormingDeleted(entryId: String) = Unit

    override suspend fun handleWeightSaved(petId: String, petName: String) = Unit

    override suspend fun cancelWeightTask(petId: String) = Unit
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val testDispatcher: TestDispatcher = StandardTestDispatcher()) :
  TestWatcher() {
  override fun starting(description: Description) {
    Dispatchers.setMain(testDispatcher)
  }

  override fun finished(description: Description) {
    Dispatchers.resetMain()
  }
}
