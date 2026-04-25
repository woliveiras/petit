package com.woliveiras.petit.data.mapper

import com.woliveiras.petit.data.local.entity.VaccinationEntryEntity
import com.woliveiras.petit.domain.model.SyncStatus
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import java.time.Instant
import java.time.ZoneId

/** Mappers for VaccinationEntry entity <-> domain model conversions. */
fun VaccinationEntryEntity.toDomain(): VaccinationEntry =
  VaccinationEntry(
    id = id,
    petId = petId,
    vaccineType =
      try {
        VaccineType.valueOf(vaccineType)
      } catch (_: Exception) {
        VaccineType.OTHER
      },
    customVaccineTypeName = customVaccineTypeName,
    applicationDate =
      Instant.ofEpochMilli(applicationDate).atZone(ZoneId.systemDefault()).toLocalDate(),
    nextDueDate =
      nextDueDate?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
    veterinarian = veterinarian,
    clinic = clinic,
    batchNumber = batchNumber,
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

fun VaccinationEntry.toEntity(): VaccinationEntryEntity =
  VaccinationEntryEntity(
    id = id,
    petId = petId,
    vaccineType = vaccineType.name,
    customVaccineTypeName = customVaccineTypeName,
    applicationDate =
      applicationDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    nextDueDate = nextDueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
    veterinarian = veterinarian,
    clinic = clinic,
    batchNumber = batchNumber,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = syncStatus.name,
  )

fun List<VaccinationEntryEntity>.toDomain(): List<VaccinationEntry> = map { it.toDomain() }
