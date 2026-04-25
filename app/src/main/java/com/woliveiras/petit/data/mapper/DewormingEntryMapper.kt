package com.woliveiras.petit.data.mapper

import com.woliveiras.petit.data.local.entity.DewormingEntryEntity
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.SyncStatus
import java.time.Instant
import java.time.ZoneId

/** Mappers for DewormingEntry entity <-> domain model conversions. */
fun DewormingEntryEntity.toDomain(): DewormingEntry =
  DewormingEntry(
    id = id,
    petId = petId,
    type =
      try {
        DewormingType.valueOf(type)
      } catch (_: Exception) {
        DewormingType.INTERNAL
      },
    medication = medication.ifBlank { null },
    applicationDate =
      Instant.ofEpochMilli(applicationDate).atZone(ZoneId.systemDefault()).toLocalDate(),
    nextDueDate =
      nextDueDate?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus =
      try {
        SyncStatus.valueOf(syncStatus)
      } catch (_: Exception) {
        SyncStatus.LOCAL_ONLY
      },
  )

fun DewormingEntry.toEntity(): DewormingEntryEntity =
  DewormingEntryEntity(
    id = id,
    petId = petId,
    type = type.name,
    medication = medication ?: "",
    applicationDate =
      applicationDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    nextDueDate = nextDueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = syncStatus.name,
  )

fun List<DewormingEntryEntity>.toDomain(): List<DewormingEntry> = map { it.toDomain() }
