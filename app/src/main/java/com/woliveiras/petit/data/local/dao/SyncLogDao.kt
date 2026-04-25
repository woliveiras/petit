package com.woliveiras.petit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.woliveiras.petit.data.local.entity.SyncLogEntity
import kotlinx.coroutines.flow.Flow

/** Data Access Object for SyncLog entities. */
@Dao
interface SyncLogDao {

  /** Get all sync logs ordered by most recent first. */
  @Query("SELECT * FROM sync_logs ORDER BY syncTimestamp DESC")
  fun getAllSyncLogs(): Flow<List<SyncLogEntity>>

  /** Get sync logs for a specific peer. */
  @Query("SELECT * FROM sync_logs WHERE peerId = :peerId ORDER BY syncTimestamp DESC")
  fun getSyncLogsByPeer(peerId: String): Flow<List<SyncLogEntity>>

  /** Get sync logs since a specific timestamp. */
  @Query("SELECT * FROM sync_logs WHERE syncTimestamp > :since ORDER BY syncTimestamp DESC")
  suspend fun getSyncLogsSince(since: Long): List<SyncLogEntity>

  /** Get the most recent sync log. */
  @Query("SELECT * FROM sync_logs ORDER BY syncTimestamp DESC LIMIT 1")
  suspend fun getLatestSyncLog(): SyncLogEntity?

  /** Insert a new sync log. */
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSyncLog(syncLog: SyncLogEntity)

  /** Delete all sync logs. */
  @Query("DELETE FROM sync_logs") suspend fun deleteAll()
}
