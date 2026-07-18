package com.woliveiras.petit.presentation.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.data.repository.BackupAttemptCursor
import com.woliveiras.petit.data.repository.BackupAttemptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupHistoryUiState(
  val attempts: List<BackupAttempt> = emptyList(),
  val isLoading: Boolean = true,
  val canLoadMore: Boolean = false,
  val loadFailed: Boolean = false,
)

@HiltViewModel
class BackupHistoryViewModel
@Inject
internal constructor(private val repository: BackupAttemptRepository) : ViewModel() {
  private val mutableUiState = MutableStateFlow(BackupHistoryUiState())
  val uiState: StateFlow<BackupHistoryUiState> = mutableUiState.asStateFlow()
  private var nextCursor: BackupAttemptCursor? = null

  init {
    loadPage()
  }

  fun loadMore() {
    if (!mutableUiState.value.canLoadMore || mutableUiState.value.isLoading) return
    loadPage()
  }

  fun retry() {
    if (mutableUiState.value.isLoading) return
    loadPage()
  }

  private fun loadPage() {
    mutableUiState.update { it.copy(isLoading = true, loadFailed = false) }
    viewModelScope.launch {
      try {
        val page = repository.getPage(after = nextCursor, limit = HISTORY_PAGE_SIZE)
        nextCursor = page.nextCursor
        mutableUiState.update { state ->
          state.copy(
            attempts = (state.attempts + page.items).distinctBy { it.id },
            isLoading = false,
            canLoadMore = page.nextCursor != null,
          )
        }
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (_: Exception) {
        mutableUiState.update { it.copy(isLoading = false, loadFailed = true) }
      }
    }
  }

  private companion object {
    const val HISTORY_PAGE_SIZE = 5
  }
}
