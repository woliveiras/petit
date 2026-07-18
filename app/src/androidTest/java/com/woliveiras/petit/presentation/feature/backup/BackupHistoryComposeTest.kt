package com.woliveiras.petit.presentation.feature.backup

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptStatus
import com.woliveiras.petit.domain.backup.BackupTrigger
import java.time.Instant
import org.junit.Rule
import org.junit.Test

class BackupHistoryComposeTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun loadMoreUpdatesTheHistoryAndDisappearsAtTheEnd() {
    var state by
      mutableStateOf(
        BackupHistoryUiState(attempts = attempts(5), isLoading = false, canLoadMore = true)
      )
    compose.setContent {
      MaterialTheme {
        BackupHistoryScreen(
          state = state,
          copy = copy(),
          onNavigateBack = {},
          onLoadMore = {
            state =
              BackupHistoryUiState(attempts = attempts(7), isLoading = false, canLoadMore = false)
          },
          onRetry = {},
        )
      }
    }

    compose.onNodeWithText("Attempt 5").assertIsDisplayed()
    compose.onNodeWithText("Load more").performClick()

    compose.waitForIdle()
    assertThat(state.attempts).hasSize(7)
    compose.onAllNodesWithText("Load more").assertCountEquals(0)
  }

  @Test
  fun loadingEmptyAndFailureStatesAreActionable() {
    var state by mutableStateOf(BackupHistoryUiState())
    var retryCalls = 0
    compose.setContent {
      MaterialTheme {
        BackupHistoryScreen(
          state = state,
          copy = copy(),
          onNavigateBack = {},
          onLoadMore = {},
          onRetry = { retryCalls += 1 },
        )
      }
    }

    compose.onNodeWithText("Loading history").assertIsDisplayed()

    state = BackupHistoryUiState(isLoading = false)
    compose.onNodeWithText("No attempts").assertIsDisplayed()

    state = BackupHistoryUiState(isLoading = false, loadFailed = true)
    compose.onNodeWithText("Could not load history").assertIsDisplayed()
    compose.onNodeWithText("Retry").performClick()

    assertThat(retryCalls).isEqualTo(1)
  }

  private fun copy() =
    BackupHistoryCopy(
      title = "Backup history",
      noHistory = "No attempts",
      loading = "Loading history",
      loadFailed = "Could not load history",
      retry = "Retry",
      loadMore = "Load more",
      attemptSummary = { "Attempt ${it.id.removePrefix("attempt-").toInt()}" },
    )

  private fun attempts(count: Int): List<BackupAttempt> =
    (1..count).map { index ->
      BackupAttempt(
        id = "attempt-$index",
        trigger = BackupTrigger.AUTOMATIC,
        startedAt = Instant.EPOCH.plusSeconds(index.toLong()),
        status = BackupAttemptStatus.SUCCEEDED,
      )
    }
}
