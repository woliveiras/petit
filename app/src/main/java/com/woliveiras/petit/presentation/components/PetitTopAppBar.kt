package com.woliveiras.petit.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.woliveiras.petit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetitTopAppBar(
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  onNavigateBack: (() -> Unit)? = null,
  actions: @Composable () -> Unit = {},
) {
  TopAppBar(
    title = title,
    modifier = modifier,
    navigationIcon = {
      if (onNavigateBack != null) {
        IconButton(onClick = onNavigateBack) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
          )
        }
      }
    },
    actions = { actions() },
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
        titleContentColor = MaterialTheme.colorScheme.primary,
        navigationIconContentColor = MaterialTheme.colorScheme.primary,
        actionIconContentColor = MaterialTheme.colorScheme.primary,
      ),
  )
}
