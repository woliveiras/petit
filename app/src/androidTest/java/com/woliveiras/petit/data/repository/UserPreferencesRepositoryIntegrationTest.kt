package com.woliveiras.petit.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPreferencesRepositoryIntegrationTest {

  private lateinit var file: File
  private lateinit var scope: CoroutineScope

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    file = context.cacheDir.resolve("user-preferences-${System.nanoTime()}.preferences_pb")
    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }

  @After
  fun tearDown() {
    scope.cancel()
    file.delete()
  }

  @Test
  fun supportedValuesPersistAcrossRepositoryInstances() = runTest {
    val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    val repository = UserPreferencesRepositoryImpl(dataStore)

    repository.updateTheme(AppTheme.DARK)
    repository.updateLanguage(AppLanguage.PORTUGUESE_BR)

    scope.cancel()
    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val reopenedDataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    val restored = UserPreferencesRepositoryImpl(reopenedDataStore).userPreferences.first()
    assertThat(restored.theme).isEqualTo(AppTheme.DARK)
    assertThat(restored.language).isEqualTo(AppLanguage.PORTUGUESE_BR)
  }

  @Test
  fun unknownStoredValuesFallbackToSystem() = runTest {
    val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    dataStore.edit { preferences ->
      preferences[stringPreferencesKey("theme")] = "FUTURE_THEME"
      preferences[stringPreferencesKey("language")] = "es-MX"
    }

    val preferences = UserPreferencesRepositoryImpl(dataStore).userPreferences.first()

    assertThat(preferences.theme).isEqualTo(AppTheme.SYSTEM)
    assertThat(preferences.language).isEqualTo(AppLanguage.SYSTEM)
  }
}
