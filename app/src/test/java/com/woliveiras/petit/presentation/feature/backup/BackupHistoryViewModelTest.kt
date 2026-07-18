package com.woliveiras.petit.presentation.feature.backup

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptCursor
import com.woliveiras.petit.data.repository.BackupAttemptPage
import com.woliveiras.petit.data.repository.BackupAttemptRepository
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.domain.backup.BackupTrigger
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupHistoryViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun openingHistoryLoadsOnlyTheFirstFiveAttempts() =
    runTest(dispatcher) {
      val repository = FakeAttemptRepository(attempts(12))
      val viewModel = BackupHistoryViewModel(repository)

      advanceUntilIdle()

      assertThat(viewModel.uiState.value.attempts.map { it.id })
        .containsExactly("attempt-12", "attempt-11", "attempt-10", "attempt-09", "attempt-08")
        .inOrder()
      assertThat(viewModel.uiState.value.isLoading).isFalse()
      assertThat(viewModel.uiState.value.canLoadMore).isTrue()
    }

  @Test
  fun loadMoreAppendsFiveUntilTheEndWithoutDuplicates() =
    runTest(dispatcher) {
      val viewModel = BackupHistoryViewModel(FakeAttemptRepository(attempts(12)))
      advanceUntilIdle()

      viewModel.loadMore()
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.attempts.map { it.id })
        .containsExactly(
          "attempt-12",
          "attempt-11",
          "attempt-10",
          "attempt-09",
          "attempt-08",
          "attempt-07",
          "attempt-06",
          "attempt-05",
          "attempt-04",
          "attempt-03",
        )
        .inOrder()
      assertThat(viewModel.uiState.value.attempts.map { it.id }).containsNoDuplicates()
      assertThat(viewModel.uiState.value.canLoadMore).isTrue()

      viewModel.loadMore()
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.attempts).hasSize(12)
      assertThat(viewModel.uiState.value.canLoadMore).isFalse()
    }

  @Test
  fun failedPageCanBeRetriedWithoutLosingHistory() =
    runTest(dispatcher) {
      val repository = FakeAttemptRepository(attempts(7), failuresRemaining = 1)
      val viewModel = BackupHistoryViewModel(repository)
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.loadFailed).isTrue()
      assertThat(viewModel.uiState.value.attempts).isEmpty()

      viewModel.retry()
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.loadFailed).isFalse()
      assertThat(viewModel.uiState.value.attempts).hasSize(5)
      assertThat(viewModel.uiState.value.canLoadMore).isTrue()
    }

  private class FakeAttemptRepository(
    initial: List<BackupAttempt>,
    private var failuresRemaining: Int = 0,
  ) : BackupAttemptRepository {
    private val state = MutableStateFlow(initial)
    override val attempts: Flow<List<BackupAttempt>> = state

    override suspend fun getPage(after: BackupAttemptCursor?, limit: Int): BackupAttemptPage {
      if (failuresRemaining > 0) {
        failuresRemaining -= 1
        error("scripted history failure")
      }
      return super<BackupAttemptRepository>.getPage(after, limit)
    }

    override suspend fun getAttempt(id: String): BackupAttempt? =
      state.value.firstOrNull { it.id == id }

    override suspend fun upsert(attempt: BackupAttempt) {
      state.value = state.value.filterNot { it.id == attempt.id } + attempt
    }
  }

  private companion object {
    fun attempts(count: Int) =
      (1..count).map { index ->
        BackupAttempt(
          id = "attempt-${index.toString().padStart(2, '0')}",
          trigger = BackupTrigger.AUTOMATIC,
          startedAt = Instant.EPOCH.plusSeconds(index.toLong()),
          status = BackupAttemptStatus.SUCCEEDED,
        )
      }
  }
}
