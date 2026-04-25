package com.woliveiras.petit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Raw timeline event from database query. Used as intermediate representation before converting to
 * domain model.
 */
data class RawTimelineEvent(
  val eventType: String,
  val id: String,
  val petId: String?,
  val petName: String,
  val petPhotoUri: String?,
  val eventDate: Long,
  val title: String,
  val subtitle: String?,
  val isFuture: Int, // 0 = past, 1 = future
)

/**
 * Data Access Object for timeline queries. Aggregates events from multiple tables into a unified
 * timeline.
 */
@Dao
interface TimelineDao {

  /**
   * Get recent past events (last 5 of each type per pet). Ordered by date descending (most recent
   * first).
   */
  @Query(
    """
    SELECT * FROM (
      -- Past weight entries (last 5 per pet)
      SELECT 
        'WEIGHT' as eventType,
        w.id,
        w.petId,
        p.name as petName,
        p.photoUri as petPhotoUri,
        w.date as eventDate,
        'WEIGHT' as title,
        CAST(w.weightGrams AS TEXT) as subtitle,
        0 as isFuture
      FROM weight_entries w
      INNER JOIN pets p ON p.id = w.petId
      WHERE w.deletedAt IS NULL AND p.deletedAt IS NULL
        AND w.id IN (
          SELECT w2.id FROM weight_entries w2 
          WHERE w2.petId = w.petId AND w2.deletedAt IS NULL
          ORDER BY w2.date DESC LIMIT 5
        )
      
      UNION ALL
      
      -- Past vaccination entries (last 5 per pet)
      SELECT 
        'VACCINATION' as eventType,
        v.id,
        v.petId,
        p.name as petName,
        p.photoUri as petPhotoUri,
        v.applicationDate as eventDate,
        'VACCINATION' as title,
        v.vaccineType as subtitle,
        0 as isFuture
      FROM vaccination_entries v
      INNER JOIN pets p ON p.id = v.petId
      WHERE v.deletedAt IS NULL AND p.deletedAt IS NULL
        AND v.id IN (
          SELECT v2.id FROM vaccination_entries v2 
          WHERE v2.petId = v.petId AND v2.deletedAt IS NULL
          ORDER BY v2.applicationDate DESC LIMIT 5
        )
      
      UNION ALL
      
      -- Past deworming entries (last 5 per pet)
      SELECT 
        'DEWORMING' as eventType,
        d.id,
        d.petId,
        p.name as petName,
        p.photoUri as petPhotoUri,
        d.applicationDate as eventDate,
        'DEWORMING' as title,
        d.medication as subtitle,
        0 as isFuture
      FROM deworming_entries d
      INNER JOIN pets p ON p.id = d.petId
      WHERE d.deletedAt IS NULL AND p.deletedAt IS NULL
        AND d.id IN (
          SELECT d2.id FROM deworming_entries d2 
          WHERE d2.petId = d.petId AND d2.deletedAt IS NULL
          ORDER BY d2.applicationDate DESC LIMIT 5
        )
    )
    ORDER BY eventDate DESC
    """
  )
  fun getRecentActivity(): Flow<List<RawTimelineEvent>>

  /** Get upcoming events within the next N days. Ordered by date ascending (soonest first). */
  @Query(
    """
    SELECT * FROM (
      -- Upcoming vaccinations
      SELECT 
        'VACCINATION_DUE' as eventType,
        v.id,
        v.petId,
        p.name as petName,
        p.photoUri as petPhotoUri,
        v.nextDueDate as eventDate,
        'VACCINATION_DUE' as title,
        v.vaccineType as subtitle,
        1 as isFuture
      FROM vaccination_entries v
      INNER JOIN pets p ON p.id = v.petId
      WHERE v.deletedAt IS NULL 
        AND p.deletedAt IS NULL
        AND v.nextDueDate IS NOT NULL
        AND v.nextDueDate >= :today
        AND v.nextDueDate <= :futureLimit
      
      UNION ALL
      
      -- Upcoming dewormings
      SELECT 
        'DEWORMING_DUE' as eventType,
        d.id,
        d.petId,
        p.name as petName,
        p.photoUri as petPhotoUri,
        d.nextDueDate as eventDate,
        'DEWORMING_DUE' as title,
        d.medication as subtitle,
        1 as isFuture
      FROM deworming_entries d
      INNER JOIN pets p ON p.id = d.petId
      WHERE d.deletedAt IS NULL 
        AND p.deletedAt IS NULL
        AND d.nextDueDate IS NOT NULL
        AND d.nextDueDate >= :today
        AND d.nextDueDate <= :futureLimit
      
      UNION ALL
      
      -- Pending tasks
      SELECT 
        'TASK' as eventType,
        r.id,
        r.petId,
        COALESCE(p.name, '') as petName,
        p.photoUri as petPhotoUri,
        r.scheduledFor as eventDate,
        r.title as title,
        r.description as subtitle,
        1 as isFuture
      FROM tasks r
      LEFT JOIN pets p ON p.id = r.petId
      WHERE r.status = 'PENDING'
        AND r.deletedAt IS NULL
        AND r.scheduledFor >= :today
        AND r.scheduledFor <= :futureLimit
    )
    ORDER BY eventDate ASC
    """
  )
  fun getUpcomingEvents(today: Long, futureLimit: Long): Flow<List<RawTimelineEvent>>

  /** Get overdue events (past due dates that weren't completed). */
  @Query(
    """
    SELECT * FROM (
      -- Overdue vaccinations: only the most recent entry per (pet, vaccineType)
      SELECT 
        'VACCINATION_DUE' as eventType,
        v.id,
        v.petId,
        p.name as petName,
        p.photoUri as petPhotoUri,
        v.nextDueDate as eventDate,
        'VACCINATION_DUE' as title,
        v.vaccineType as subtitle,
        1 as isFuture
      FROM vaccination_entries v
      INNER JOIN pets p ON p.id = v.petId
      WHERE v.deletedAt IS NULL 
        AND p.deletedAt IS NULL
        AND v.nextDueDate IS NOT NULL
        AND v.nextDueDate < :today
        AND v.applicationDate = (
          SELECT MAX(v2.applicationDate) FROM vaccination_entries v2
          WHERE v2.petId = v.petId
            AND v2.vaccineType = v.vaccineType
            AND v2.deletedAt IS NULL
        )
      
      UNION ALL
      
      -- Overdue dewormings: only the most recent entry per pet
      SELECT 
        'DEWORMING_DUE' as eventType,
        d.id,
        d.petId,
        p.name as petName,
        p.photoUri as petPhotoUri,
        d.nextDueDate as eventDate,
        'DEWORMING_DUE' as title,
        d.medication as subtitle,
        1 as isFuture
      FROM deworming_entries d
      INNER JOIN pets p ON p.id = d.petId
      WHERE d.deletedAt IS NULL 
        AND p.deletedAt IS NULL
        AND d.nextDueDate IS NOT NULL
        AND d.nextDueDate < :today
        AND d.applicationDate = (
          SELECT MAX(d2.applicationDate) FROM deworming_entries d2
          WHERE d2.petId = d.petId
            AND d2.deletedAt IS NULL
        )
    )
    ORDER BY eventDate DESC
    """
  )
  fun getOverdueEvents(today: Long): Flow<List<RawTimelineEvent>>

  /**
   * Get all past activity events within a date range. No per-pet limit. Ordered by date descending.
   */
  @Query(
    """
    SELECT * FROM (
      SELECT 
        'WEIGHT' as eventType,
        w.id,
        w.petId,
        p.name as petName,
        p.photoUri as petPhotoUri,
        w.date as eventDate,
        'WEIGHT' as title,
        CAST(w.weightGrams AS TEXT) as subtitle,
        0 as isFuture
      FROM weight_entries w
      INNER JOIN pets p ON p.id = w.petId
      WHERE w.deletedAt IS NULL AND p.deletedAt IS NULL
        AND w.date >= :startDate AND w.date <= :endDate
      
      UNION ALL
      
      SELECT 
        'VACCINATION' as eventType,
        v.id,
        v.petId,
        p.name as petName,
        p.photoUri as petPhotoUri,
        v.applicationDate as eventDate,
        'VACCINATION' as title,
        v.vaccineType as subtitle,
        0 as isFuture
      FROM vaccination_entries v
      INNER JOIN pets p ON p.id = v.petId
      WHERE v.deletedAt IS NULL AND p.deletedAt IS NULL
        AND v.applicationDate >= :startDate AND v.applicationDate <= :endDate
      
      UNION ALL
      
      SELECT 
        'DEWORMING' as eventType,
        d.id,
        d.petId,
        p.name as petName,
        p.photoUri as petPhotoUri,
        d.applicationDate as eventDate,
        'DEWORMING' as title,
        d.medication as subtitle,
        0 as isFuture
      FROM deworming_entries d
      INNER JOIN pets p ON p.id = d.petId
      WHERE d.deletedAt IS NULL AND p.deletedAt IS NULL
        AND d.applicationDate >= :startDate AND d.applicationDate <= :endDate
    )
    ORDER BY eventDate DESC
    """
  )
  fun getAllActivity(startDate: Long, endDate: Long): Flow<List<RawTimelineEvent>>
}
