package com.woliveiras.petit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** Room entity representing a sync operation log entry. */
@Entity(tableName = "sync_logs")
data class SyncLogEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val peerId: String,
  val peerName: String,
  val syncTimestamp: Long,
  val entitiesSent: Int,
  val entitiesReceived: Int,
  val conflictsResolved: Int,
  val syncType: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  // TODO: deletedAt and syncStatus are vestigial (sync logs are never soft-deleted or synced).
  //  Remove in a future schema migration.
  val deletedAt: Long? = null,
  val syncStatus: String = "LOCAL_ONLY",
)
