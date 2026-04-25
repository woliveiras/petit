package com.woliveiras.petit.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.woliveiras.petit.data.local.dao.DewormingEntryDao
import com.woliveiras.petit.data.local.dao.FamilyGroupMemberDao
import com.woliveiras.petit.data.local.dao.PetDao
import com.woliveiras.petit.data.local.dao.SyncLogDao
import com.woliveiras.petit.data.local.dao.TaskDao
import com.woliveiras.petit.data.local.dao.TimelineDao
import com.woliveiras.petit.data.local.dao.VaccinationEntryDao
import com.woliveiras.petit.data.local.dao.WeightEntryDao
import com.woliveiras.petit.data.local.entity.DewormingEntryEntity
import com.woliveiras.petit.data.local.entity.FamilyGroupMemberEntity
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.data.local.entity.SyncLogEntity
import com.woliveiras.petit.data.local.entity.TaskEntity
import com.woliveiras.petit.data.local.entity.VaccinationEntryEntity
import com.woliveiras.petit.data.local.entity.WeightEntryEntity

/** Main Room database for Petit app. */
@Database(
  entities =
    [
      PetEntity::class,
      WeightEntryEntity::class,
      VaccinationEntryEntity::class,
      DewormingEntryEntity::class,
      TaskEntity::class,
      FamilyGroupMemberEntity::class,
      SyncLogEntity::class,
    ],
  version = 1,
  exportSchema = true,
)
abstract class PetitDatabase : RoomDatabase() {

  abstract fun petDao(): PetDao

  abstract fun weightEntryDao(): WeightEntryDao

  abstract fun vaccinationEntryDao(): VaccinationEntryDao

  abstract fun dewormingEntryDao(): DewormingEntryDao

  abstract fun taskDao(): TaskDao

  abstract fun timelineDao(): TimelineDao

  abstract fun familyGroupMemberDao(): FamilyGroupMemberDao

  abstract fun syncLogDao(): SyncLogDao

  companion object {
    const val DATABASE_NAME = "petit_database"
  }
}
