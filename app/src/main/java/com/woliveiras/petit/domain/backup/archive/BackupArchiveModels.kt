package com.woliveiras.petit.domain.backup.archive

import com.woliveiras.petit.domain.backup.BackupTrigger
import com.woliveiras.petit.domain.model.AppLanguage
import com.woliveiras.petit.domain.model.AppTheme
import com.woliveiras.petit.domain.model.ExportBundle
import java.io.Closeable
import java.io.File
import java.time.Instant

data class BackupAppPreferences(
  val theme: AppTheme,
  val language: AppLanguage,
  val hasCompletedOnboarding: Boolean,
)

data class BackupReminderPreferences(
  val vaccinationRemindersEnabled: Boolean,
  val vaccinationDaysBefore: Int,
  val dewormingRemindersEnabled: Boolean,
  val dewormingDaysBefore: Int,
  val weightRemindersEnabled: Boolean,
  val weightReminderIntervalDays: Int,
  val defaultNotificationHour: Int,
  val defaultNotificationMinute: Int,
)

/** An app-owned asset. The archive path is derived from trusted IDs instead of source URIs. */
data class BackupAsset(
  val petId: String,
  val fileName: String,
  val mediaType: String,
  val source: File,
)

data class BackupSnapshot(
  val exportBundle: ExportBundle,
  val appPreferences: BackupAppPreferences,
  val reminderPreferences: BackupReminderPreferences,
  val assets: List<BackupAsset>,
)

data class BackupArchiveRequest(
  val backupId: String,
  val createdAt: Instant,
  val appVersion: String,
  val trigger: BackupTrigger,
  val snapshot: BackupSnapshot,
  val outputDirectory: File,
)

data class BackupPayloadEntry(
  val path: String,
  val uncompressedSize: Long,
  val mediaType: String,
  val sha256: String,
)

data class BackupManifest(
  val archiveFormatVersion: Int,
  val dataSchemaVersion: Int,
  val backupId: String,
  val createdAt: Instant,
  val appVersion: String,
  val trigger: BackupTrigger,
  val entryCounts: Map<String, Int>,
  val entries: List<BackupPayloadEntry>,
  val payloadCount: Int,
  val totalUncompressedPayloadSize: Long,
)

data class BackupArchiveResult(
  val file: File,
  val backupId: String,
  val byteSize: Long,
  val sha256: String,
  val manifest: BackupManifest,
)

/** A fully validated archive whose extracted assets exist only until [close] is called. */
class ValidatedBackupArchive
internal constructor(
  val manifest: BackupManifest,
  val snapshot: BackupSnapshot,
  private val stagingDirectory: File,
) : Closeable {
  override fun close() {
    stagingDirectory.deleteRecursively()
  }
}

data class BackupArchiveLimits(
  val maxCompressedArchiveBytes: Long = 128L * 1024L * 1024L,
  val maxTotalUncompressedBytes: Long = 256L * 1024L * 1024L,
  val maxEntryBytes: Long = 16L * 1024L * 1024L,
  val maxEntryCount: Int = 2_048,
  val maxManifestBytes: Long = 2L * 1024L * 1024L,
  val maxPathLength: Int = 240,
  val maxCompressionRatio: Long = 200L,
)

class BackupArchiveException(message: String, cause: Throwable? = null) :
  IllegalArgumentException(message, cause)
