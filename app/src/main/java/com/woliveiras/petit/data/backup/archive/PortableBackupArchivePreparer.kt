package com.woliveiras.petit.data.backup.archive

import android.content.Context
import android.net.Uri
import com.woliveiras.petit.BuildConfig
import com.woliveiras.petit.data.media.PetPhotoStorage
import com.woliveiras.petit.data.repository.ReminderPreferencesRepository
import com.woliveiras.petit.data.repository.UserPreferencesRepository
import com.woliveiras.petit.domain.backup.BackupContent
import com.woliveiras.petit.domain.backup.BackupContentCounts
import com.woliveiras.petit.domain.backup.BackupMetadata
import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.backup.archive.BackupAppPreferences
import com.woliveiras.petit.domain.backup.archive.BackupArchiveCodec
import com.woliveiras.petit.domain.backup.archive.BackupArchiveRequest
import com.woliveiras.petit.domain.backup.archive.BackupArchiveResult
import com.woliveiras.petit.domain.backup.archive.BackupAsset
import com.woliveiras.petit.domain.backup.archive.BackupReminderPreferences
import com.woliveiras.petit.domain.backup.archive.BackupSnapshot
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.usecase.ExportImportUseCase
import com.woliveiras.petit.domain.usecase.backup.BackupArchivePreparer
import com.woliveiras.petit.domain.usecase.backup.PreparedBackupArchive
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Reads only app-owned pet assets and copies them into disposable backup staging. */
interface BackupPetAssetSource {
  fun stage(pet: Pet, stagingDirectory: File): BackupAsset?
}

class AndroidBackupPetAssetSource
@Inject
constructor(@param:ApplicationContext private val context: Context) : BackupPetAssetSource {
  override fun stage(pet: Pet, stagingDirectory: File): BackupAsset? {
    val uri = pet.photoUri?.let(Uri::parse) ?: return null
    require(
      uri.scheme == "content" && uri.authority == "${BuildConfig.APPLICATION_ID}.fileprovider"
    ) {
      "Only app-owned pet assets can be backed up"
    }
    val mediaType = context.contentResolver.getType(uri)
    val extension =
      when (mediaType) {
        JPEG_MEDIA_TYPE -> "jpg"
        PNG_MEDIA_TYPE -> "png"
        else -> throw IllegalArgumentException("Unsupported pet asset media type")
      }
    require(stagingDirectory.mkdirs() || stagingDirectory.isDirectory) {
      "Asset staging directory is unavailable"
    }
    val destination = stagingDirectory.resolve("${UUID.randomUUID()}.$extension")
    try {
      context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Pet asset is unavailable" }
        destination.outputStream().use { output ->
          val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
          var total = 0L
          while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= PetPhotoStorage.MAX_IMAGE_BYTES) { "Pet asset exceeds its size limit" }
            output.write(buffer, 0, read)
          }
          require(total > 0) { "Pet asset is empty" }
        }
      }
      return BackupAsset(pet.id, "photo.$extension", checkNotNull(mediaType), destination)
    } catch (error: Exception) {
      destination.delete()
      throw error
    }
  }

  private companion object {
    const val JPEG_MEDIA_TYPE = "image/jpeg"
    const val PNG_MEDIA_TYPE = "image/png"
  }
}

/** Provider-neutral archive preparation adapter used by manual and background backup flows. */
class PortableBackupArchivePreparer(
  private val exportImportUseCase: ExportImportUseCase,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val reminderPreferencesRepository: ReminderPreferencesRepository,
  private val assetSource: BackupPetAssetSource,
  private val codec: BackupArchiveCodec,
  private val stagingRoot: File,
  private val appVersion: String,
  private val clock: Clock = Clock.systemUTC(),
) : BackupArchivePreparer {
  override suspend fun prepare(backupId: String, trigger: BackupTrigger): PreparedBackupArchive {
    val operationDirectory = stagingRoot.resolve("backup-${UUID.randomUUID()}")
    try {
      check(operationDirectory.mkdirs()) { "Could not create backup operation staging" }
      val bundle = exportImportUseCase.exportBackupSnapshot()
      val assetDirectory = operationDirectory.resolve("source-assets")
      val assets = bundle.pets.mapNotNull { pet -> assetSource.stage(pet, assetDirectory) }
      val appPreferences = userPreferencesRepository.userPreferences.first()
      val reminders = reminderPreferencesRepository.getPreferences()
      val result =
        codec.create(
          BackupArchiveRequest(
            backupId = backupId,
            createdAt = Instant.now(clock),
            appVersion = appVersion,
            trigger = trigger,
            snapshot =
              BackupSnapshot(
                exportBundle = bundle,
                appPreferences =
                  BackupAppPreferences(
                    theme = appPreferences.theme,
                    language = appPreferences.language,
                    hasCompletedOnboarding = appPreferences.hasCompletedOnboarding,
                  ),
                reminderPreferences =
                  BackupReminderPreferences(
                    vaccinationRemindersEnabled = reminders.vaccinationRemindersEnabled,
                    vaccinationDaysBefore = reminders.vaccinationDaysBefore,
                    dewormingRemindersEnabled = reminders.dewormingRemindersEnabled,
                    dewormingDaysBefore = reminders.dewormingDaysBefore,
                    weightRemindersEnabled = reminders.weightRemindersEnabled,
                    weightReminderIntervalDays = reminders.weightReminderIntervalDays,
                    defaultNotificationHour = reminders.defaultNotificationHour,
                    defaultNotificationMinute = reminders.defaultNotificationMinute,
                  ),
                assets = assets,
              ),
            outputDirectory = operationDirectory.resolve("archive"),
          )
        )
      return FilePreparedBackupArchive(result, operationDirectory)
    } catch (error: Exception) {
      operationDirectory.deleteRecursively()
      throw error
    }
  }

  private class FilePreparedBackupArchive(
    result: BackupArchiveResult,
    private val operationDirectory: File,
  ) : PreparedBackupArchive {
    override val metadata =
      BackupMetadata(
        remoteId = "",
        backupId = result.backupId,
        createdAt = result.manifest.createdAt,
        trigger = result.manifest.trigger,
        appVersion = result.manifest.appVersion,
        archiveFormatVersion = result.manifest.archiveFormatVersion,
        schemaVersion = result.manifest.dataSchemaVersion,
        contentCounts =
          BackupContentCounts(
            pets = result.manifest.entryCounts.getValue("pets"),
            weights = result.manifest.entryCounts.getValue("weights"),
            vaccinations = result.manifest.entryCounts.getValue("vaccinations"),
            dewormingRecords = result.manifest.entryCounts.getValue("dewormings"),
            tasks = result.manifest.entryCounts.getValue("tasks"),
            assets = result.manifest.entryCounts.getValue("assets"),
          ),
        archiveSizeBytes = result.byteSize,
        archiveSha256 = result.sha256,
      )
    override val content: BackupContent = FileBackupContent(result.file, result.byteSize)

    override fun close() {
      operationDirectory.deleteRecursively()
    }
  }

  private class FileBackupContent(private val file: File, override val byteSize: Long) :
    BackupContent {
    override fun openInputStream(): InputStream {
      check(file.isFile) { "Prepared backup archive has been cleaned up" }
      return file.inputStream()
    }
  }
}
