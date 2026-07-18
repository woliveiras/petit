package com.woliveiras.petit.data.backup.archive

import android.content.Context
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.BuildConfig
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.repository.DewormingEntryRepositoryImpl
import com.woliveiras.petit.data.repository.PetRepositoryImpl
import com.woliveiras.petit.data.repository.ReminderPreferences
import com.woliveiras.petit.data.repository.ReminderPreferencesRepository
import com.woliveiras.petit.data.repository.TaskRepositoryImpl
import com.woliveiras.petit.data.repository.UserPreferences
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.data.repository.VaccinationEntryRepositoryImpl
import com.woliveiras.petit.data.repository.WeightEntryRepositoryImpl
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.backup.archive.BackupArchiveCodec
import com.woliveiras.petit.domain.backup.archive.BackupAsset
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.usecase.ExportImportUseCase
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PortableBackupArchivePreparerTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  private lateinit var database: PetitDatabase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java)
        .allowMainThreadQueries()
        .build()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun preparesConsistentPortableSnapshotAndCloseDeletesArchiveStaging() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database
      .petDao()
      .insertPet(
        PetEntity(
          id = "pet-1",
          name = "Mimi",
          photoUri = "content://app-owned/photo",
          createdAt = 1L,
          updatedAt = 2L,
        )
      )
    val exportImport = createExportImport(context)
    val codec = BackupArchiveCodec()
    val staging = temporaryFolder.newFolder("backup-staging")
    val preparer =
      PortableBackupArchivePreparer(
        exportImportUseCase = exportImport,
        userPreferencesRepository =
          FakeUserPreferencesRepository(
            UserPreferences(AppTheme.DARK, AppLanguage.PORTUGUESE_BR, true)
          ),
        reminderPreferencesRepository =
          FakeReminderPreferencesRepository(
            ReminderPreferences(defaultNotificationHour = 7, defaultNotificationMinute = 45)
          ),
        assetSource =
          object : BackupPetAssetSource {
            override fun stage(pet: Pet, stagingDirectory: File): BackupAsset {
              val source = stagingDirectory.resolve("source.jpg")
              check(stagingDirectory.mkdirs() || stagingDirectory.isDirectory)
              source.writeBytes(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 1, 2, 3))
              return BackupAsset(pet.id, "photo.jpg", "image/jpeg", source)
            }
          },
        codec = codec,
        stagingRoot = staging,
        appVersion = "1.2.3",
        clock = Clock.fixed(Instant.parse("2026-07-18T10:15:30Z"), ZoneOffset.UTC),
      )

    val prepared = preparer.prepare("backup-1", BackupTrigger.MANUAL)
    val archiveCopy = temporaryFolder.newFile("prepared.zip")
    prepared.content.openInputStream().use { input ->
      archiveCopy.outputStream().use(input::copyTo)
    }
    val validated = codec.validate(archiveCopy)

    assertThat(prepared.metadata.backupId).isEqualTo("backup-1")
    assertThat(prepared.metadata.archiveSizeBytes).isEqualTo(archiveCopy.length())
    assertThat(prepared.metadata.contentCounts.pets).isEqualTo(1)
    assertThat(prepared.metadata.contentCounts.assets).isEqualTo(1)
    assertThat(validated.snapshot.exportBundle.pets.single().photoUri)
      .isEqualTo("assets/pets/pet-1/photo.jpg")
    assertThat(validated.snapshot.appPreferences.theme).isEqualTo(AppTheme.DARK)
    assertThat(validated.snapshot.reminderPreferences.defaultNotificationHour).isEqualTo(7)
    validated.close()
    val immutableByteSize = prepared.content.byteSize

    prepared.close()

    assertThat(staging.listFiles().orEmpty()).isEmpty()
    assertThat(prepared.content.byteSize).isEqualTo(immutableByteSize)
    val cleanedFailure = runCatching { prepared.content.openInputStream() }.exceptionOrNull()
    assertThat(cleanedFailure).isInstanceOf(IllegalStateException::class.java)
    assertThat(cleanedFailure).hasMessageThat().contains("cleaned up")
  }

  @Test
  fun assetFailureRemovesOperationDirectoryAndNeverPublishesAnArchive() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database
      .petDao()
      .insertPet(PetEntity(id = "pet-1", name = "Mimi", photoUri = "content://lost/photo"))
    val staging = temporaryFolder.newFolder("failed-backup-staging")
    val preparer =
      PortableBackupArchivePreparer(
        exportImportUseCase = createExportImport(context),
        userPreferencesRepository = FakeUserPreferencesRepository(UserPreferences()),
        reminderPreferencesRepository = FakeReminderPreferencesRepository(ReminderPreferences()),
        assetSource =
          object : BackupPetAssetSource {
            override fun stage(pet: Pet, stagingDirectory: File): BackupAsset {
              throw java.io.IOException("Simulated asset interruption")
            }
          },
        codec = BackupArchiveCodec(),
        stagingRoot = staging,
        appVersion = "1.2.3",
      )

    val failure = runCatching { preparer.prepare("backup-failure", BackupTrigger.MANUAL) }

    assertThat(failure.isFailure).isTrue()
    assertThat(staging.listFiles().orEmpty()).isEmpty()
  }

  @Test
  fun traversalShapedBackupIdCannotChooseOrEscapeTheOperationDirectory() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val staging = temporaryFolder.newFolder("malicious-id-staging")
    val outside = checkNotNull(staging.parentFile).resolve("escaped")
    val preparer =
      PortableBackupArchivePreparer(
        exportImportUseCase = createExportImport(context),
        userPreferencesRepository = FakeUserPreferencesRepository(UserPreferences()),
        reminderPreferencesRepository = FakeReminderPreferencesRepository(ReminderPreferences()),
        assetSource =
          object : BackupPetAssetSource {
            override fun stage(pet: Pet, stagingDirectory: File): BackupAsset? = null
          },
        codec = BackupArchiveCodec(),
        stagingRoot = staging,
        appVersion = "1.2.3",
      )

    val failure = runCatching { preparer.prepare("x/../../escaped", BackupTrigger.MANUAL) }

    assertThat(failure.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    assertThat(staging.listFiles().orEmpty()).isEmpty()
    assertThat(outside.exists()).isFalse()
  }

  @Test
  fun traversalShapedPetIdCannotChooseTheLocalAssetStagingPath() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val photoDirectory = context.filesDir.resolve("pet_photos").apply { mkdirs() }
    val photo = photoDirectory.resolve("malicious-source.jpg")
    photo.writeBytes(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 1))
    val uri =
      FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", photo)
    val staging = temporaryFolder.newFolder("malicious-pet-staging")
    val outside = checkNotNull(staging.parentFile).resolve("escaped.jpg")

    val asset =
      AndroidBackupPetAssetSource(context)
        .stage(
          Pet(
            id = "../escaped",
            name = "Mimi",
            photoUri = uri.toString(),
            createdAt = 1L,
            updatedAt = 1L,
          ),
          staging,
        )

    assertThat(asset?.source?.canonicalFile?.parentFile).isEqualTo(staging.canonicalFile)
    assertThat(outside.exists()).isFalse()
    photo.delete()
  }

  private fun createExportImport(context: Context) =
    ExportImportUseCase(
      context = context,
      database = database,
      petRepository = PetRepositoryImpl(database.petDao()),
      weightRepository = WeightEntryRepositoryImpl(database.weightEntryDao()),
      vaccinationRepository =
        VaccinationEntryRepositoryImpl(database.vaccinationEntryDao(), Clock.systemUTC()),
      dewormingRepository =
        DewormingEntryRepositoryImpl(database.dewormingEntryDao(), Clock.systemUTC()),
      taskRepository = TaskRepositoryImpl(database.taskDao()),
    )

  private class FakeUserPreferencesRepository(initial: UserPreferences) :
    UserPreferencesRepository {
    override val userPreferences: Flow<UserPreferences> = MutableStateFlow(initial)

    override suspend fun updateTheme(theme: AppTheme) = Unit

    override suspend fun updateLanguage(language: AppLanguage) = Unit

    override suspend fun setOnboardingCompleted() = Unit
  }

  private class FakeReminderPreferencesRepository(private val current: ReminderPreferences) :
    ReminderPreferencesRepository {
    override val preferences: Flow<ReminderPreferences> = MutableStateFlow(current)

    override suspend fun getPreferences() = current

    override suspend fun updateVaccinationSettings(enabled: Boolean, daysBefore: Int) = Unit

    override suspend fun updateDewormingSettings(enabled: Boolean, daysBefore: Int) = Unit

    override suspend fun updateWeightSettings(enabled: Boolean, intervalDays: Int) = Unit

    override suspend fun updateNotificationTime(hour: Int, minute: Int) = Unit

    override suspend fun reset() = Unit
  }
}
