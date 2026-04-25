package com.woliveiras.petit.data.mapper

import com.woliveiras.petit.data.local.entity.FamilyGroupMemberEntity
import com.woliveiras.petit.data.local.entity.SyncLogEntity
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.SyncLog

/** Mappers for FamilyGroupMember entity <-> domain model conversions. */
fun FamilyGroupMemberEntity.toDomain(): FamilyGroupMember =
  FamilyGroupMember(
    id = id,
    deviceName = deviceName,
    familyGroupKey = familyGroupKey,
    isLocalDevice = isLocalDevice,
    lastSyncAt = lastSyncAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun FamilyGroupMember.toEntity(): FamilyGroupMemberEntity =
  FamilyGroupMemberEntity(
    id = id,
    deviceName = deviceName,
    familyGroupKey = familyGroupKey,
    isLocalDevice = isLocalDevice,
    lastSyncAt = lastSyncAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

/** Mappers for SyncLog entity <-> domain model conversions. */
fun SyncLogEntity.toDomain(): SyncLog =
  SyncLog(
    id = id,
    peerId = peerId,
    peerName = peerName,
    syncTimestamp = syncTimestamp,
    entitiesSent = entitiesSent,
    entitiesReceived = entitiesReceived,
    conflictsResolved = conflictsResolved,
    syncType = syncType,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun SyncLog.toEntity(): SyncLogEntity =
  SyncLogEntity(
    id = id,
    peerId = peerId,
    peerName = peerName,
    syncTimestamp = syncTimestamp,
    entitiesSent = entitiesSent,
    entitiesReceived = entitiesReceived,
    conflictsResolved = conflictsResolved,
    syncType = syncType,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
