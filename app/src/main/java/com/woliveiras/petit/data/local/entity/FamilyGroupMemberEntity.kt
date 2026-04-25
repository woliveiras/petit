package com.woliveiras.petit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** Room entity representing a member of the family group. */
@Entity(tableName = "family_group_members")
data class FamilyGroupMemberEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val deviceName: String,
  val familyGroupKey: String,
  val isLocalDevice: Boolean = false,
  val lastSyncAt: Long? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val deletedAt: Long? = null,
  val syncStatus: String = "LOCAL_ONLY",
)
