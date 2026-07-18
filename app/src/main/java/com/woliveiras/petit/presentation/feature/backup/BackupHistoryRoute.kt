package com.woliveiras.petit.presentation.feature.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.petit.R

@Composable
fun BackupHistoryRoute(
  onNavigateBack: () -> Unit,
  viewModel: BackupHistoryViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val settingsCopy = backupSettingsCopy()
  BackupHistoryScreen(
    state = state,
    copy =
      BackupHistoryCopy(
        title = stringResource(R.string.backup_preferences_history),
        noHistory = stringResource(R.string.backup_preferences_no_history),
        loading = stringResource(R.string.backup_history_loading),
        loadFailed = stringResource(R.string.backup_history_load_failed),
        retry = stringResource(R.string.action_retry),
        loadMore = stringResource(R.string.backup_history_load_more),
        attemptSummary = settingsCopy.attemptSummary,
      ),
    onNavigateBack = onNavigateBack,
    onLoadMore = viewModel::loadMore,
    onRetry = viewModel::retry,
  )
}
