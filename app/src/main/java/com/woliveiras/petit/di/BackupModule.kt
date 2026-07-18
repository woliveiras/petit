package com.woliveiras.petit.di

import android.content.Context
import com.woliveiras.petit.BuildConfig
import com.woliveiras.petit.data.backup.archive.AndroidBackupPetAssetSource
import com.woliveiras.petit.data.backup.archive.BackupPetAssetSource
import com.woliveiras.petit.data.backup.archive.PortableBackupArchivePreparer
import com.woliveiras.petit.data.repository.ReminderPreferencesRepository
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.backup.archive.BackupArchiveCodec
import com.woliveiras.petit.domain.usecase.ExportImportUseCase
import com.woliveiras.petit.domain.usecase.backup.BackupArchivePreparer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/** Provider-independent archive dependencies shared by manual and background backup flows. */
@Module
@InstallIn(SingletonComponent::class)
object BackupModule {
  @Provides @Singleton fun provideBackupArchiveCodec(): BackupArchiveCodec = BackupArchiveCodec()

  @Provides
  @Singleton
  fun provideBackupPetAssetSource(source: AndroidBackupPetAssetSource): BackupPetAssetSource =
    source

  @Provides
  @Singleton
  fun provideBackupArchivePreparer(
    @ApplicationContext context: Context,
    exportImportUseCase: ExportImportUseCase,
    userPreferencesRepository: UserPreferencesRepository,
    reminderPreferencesRepository: ReminderPreferencesRepository,
    assetSource: BackupPetAssetSource,
    codec: BackupArchiveCodec,
    clock: Clock,
  ): BackupArchivePreparer =
    PortableBackupArchivePreparer(
      exportImportUseCase = exportImportUseCase,
      userPreferencesRepository = userPreferencesRepository,
      reminderPreferencesRepository = reminderPreferencesRepository,
      assetSource = assetSource,
      codec = codec,
      stagingRoot = context.cacheDir.resolve("backup_staging"),
      appVersion = BuildConfig.VERSION_NAME,
      clock = clock,
    )
}
