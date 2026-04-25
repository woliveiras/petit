package com.woliveiras.petit.domain.model

/** Domain model containing family group information. */
data class FamilyGroupInfo(
  val familyGroupKey: String,
  val members: List<FamilyGroupMember>,
  val createdAt: Long,
)
