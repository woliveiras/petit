package com.woliveiras.petit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Room entity representing a deworming/antiparasitic entry. */
@Entity(
  tableName = "deworming_entries",
  foreignKeys =
    [
      ForeignKey(
        entity = PetEntity::class,
        parentColumns = ["id"],
        childColumns = ["petId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("petId")],
)
data class DewormingEntryEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val petId: String,
  val type: String,
  val medication: String,
  val applicationDate: Long,
  val nextDueDate: Long? = null,
  val note: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val deletedAt: Long? = null,
  val syncStatus: String = "LOCAL_ONLY",
)
