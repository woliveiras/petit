package com.woliveiras.petit.domain.model

/** Domain model representing a member of the family group. */
data class FamilyGroupMember(
  val id: String,
  val deviceName: String,
  val familyGroupKey: String,
  val isLocalDevice: Boolean,
  val lastSyncAt: Long?,
  val createdAt: Long,
  val updatedAt: Long,
)
