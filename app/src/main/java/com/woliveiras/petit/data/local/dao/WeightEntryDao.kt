package com.woliveiras.petit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.woliveiras.petit.data.local.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

/** Data Access Object for WeightEntry entities. */
@Dao
interface WeightEntryDao {

  /** Get all weight entries for a pet, ordered by date (newest first). */
  @Query(
    "SELECT * FROM weight_entries WHERE petId = :petId AND deletedAt IS NULL ORDER BY date DESC"
  )
  fun getWeightEntriesForPet(petId: String): Flow<List<WeightEntryEntity>>

  /** Get the latest weight entry for a pet. */
  @Query(
    "SELECT * FROM weight_entries WHERE petId = :petId AND deletedAt IS NULL ORDER BY date DESC LIMIT 1"
  )
  suspend fun getLatestWeightEntry(petId: String): WeightEntryEntity?

  /** Get the latest weight entry for a pet as Flow. */
  @Query(
    "SELECT * FROM weight_entries WHERE petId = :petId AND deletedAt IS NULL ORDER BY date DESC LIMIT 1"
  )
  fun getLatestWeightEntryFlow(petId: String): Flow<WeightEntryEntity?>

  /** Get a weight entry by ID. */
  @Query("SELECT * FROM weight_entries WHERE id = :id AND deletedAt IS NULL")
  suspend fun getWeightEntryById(id: String): WeightEntryEntity?

  /** Get weight entry for a specific date (for upsert logic). */
  @Query("SELECT * FROM weight_entries WHERE petId = :petId AND date = :date AND deletedAt IS NULL")
  suspend fun getWeightEntryByDate(petId: String, date: Long): WeightEntryEntity?

  /** Insert a new weight entry. */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWeightEntry(entry: WeightEntryEntity)

  /** Update an existing weight entry. */
  @Update suspend fun updateWeightEntry(entry: WeightEntryEntity)

  /** Soft delete a weight entry. */
  @Query("UPDATE weight_entries SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
  suspend fun softDeleteWeightEntry(id: String, timestamp: Long = System.currentTimeMillis())

  /** Get weight entries for chart (limited to last N entries). */
  @Query(
    "SELECT * FROM weight_entries WHERE petId = :petId AND deletedAt IS NULL ORDER BY date ASC LIMIT :limit"
  )
  fun getWeightEntriesForChart(petId: String, limit: Int = 30): Flow<List<WeightEntryEntity>>

  /** Observe weight entry changes (lightweight - only returns count and max updatedAt). */
  @Query("SELECT MAX(updatedAt) FROM weight_entries WHERE deletedAt IS NULL")
  fun observeLatestUpdate(): Flow<Long?>

  /** Count weight entries for a pet. */
  @Query("SELECT COUNT(*) FROM weight_entries WHERE petId = :petId AND deletedAt IS NULL")
  suspend fun countWeightEntriesForPet(petId: String): Int

  /** Delete all weight entries (for data cleanup). */
  @Query("DELETE FROM weight_entries") suspend fun deleteAll()
}
