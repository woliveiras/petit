package com.woliveiras.petit.domain.model

/** Domain model representing a sync operation log entry. */
data class SyncLog(
  val id: String,
  val peerId: String,
  val peerName: String,
  val syncTimestamp: Long,
  val entitiesSent: Int,
  val entitiesReceived: Int,
  val conflictsResolved: Int,
  val syncType: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)
