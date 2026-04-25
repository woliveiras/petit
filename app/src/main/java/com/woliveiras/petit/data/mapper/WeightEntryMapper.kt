package com.woliveiras.petit.data.mapper

import com.woliveiras.petit.data.local.entity.WeightEntryEntity
import com.woliveiras.petit.domain.model.SyncStatus
import com.woliveiras.petit.domain.model.WeightEntry
import java.time.Instant
import java.time.ZoneId

/** Mappers for WeightEntry entity <-> domain model conversions. */
fun WeightEntryEntity.toDomain(): WeightEntry =
  WeightEntry(
    id = id,
    petId = petId,
    date = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate(),
    weightGrams = weightGrams,
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

fun WeightEntry.toEntity(): WeightEntryEntity =
  WeightEntryEntity(
    id = id,
    petId = petId,
    date = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    weightGrams = weightGrams,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = syncStatus.name,
  )

fun List<WeightEntryEntity>.toDomain(): List<WeightEntry> = map { it.toDomain() }
