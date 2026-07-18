package com.woliveiras.petit.presentation.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.woliveiras.petit.data.repository.BackupAttempt
import com.woliveiras.petit.presentation.components.PetitTopAppBar

data class BackupHistoryCopy(
  val title: String,
  val noHistory: String,
  val loading: String,
  val loadFailed: String,
  val retry: String,
  val loadMore: String,
  val attemptSummary: (BackupAttempt) -> String,
)

@Composable
fun BackupHistoryScreen(
  state: BackupHistoryUiState,
  copy: BackupHistoryCopy,
  onNavigateBack: () -> Unit,
  onLoadMore: () -> Unit,
  onRetry: () -> Unit,
) {
  Scaffold(
    topBar = { PetitTopAppBar(title = { Text(copy.title) }, onNavigateBack = onNavigateBack) }
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (state.attempts.isEmpty() && !state.isLoading && !state.loadFailed) {
        item { Text(copy.noHistory) }
      }
      items(state.attempts, key = { it.id }) { attempt ->
        Card(Modifier.fillMaxWidth()) {
          Text(copy.attemptSummary(attempt), Modifier.padding(12.dp))
        }
      }
      if (state.isLoading) {
        item {
          Text(copy.loading, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
      }
      if (state.loadFailed) {
        item {
          Text(copy.loadFailed, color = MaterialTheme.colorScheme.error)
          Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text(copy.retry) }
        }
      } else if (state.canLoadMore) {
        item {
          Button(
            onClick = onLoadMore,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(copy.loadMore)
          }
        }
      }
    }
  }
}
