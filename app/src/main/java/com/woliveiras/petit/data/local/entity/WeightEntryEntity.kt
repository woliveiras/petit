package com.woliveiras.petit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Room entity representing a weight measurement entry. */
@Entity(
  tableName = "weight_entries",
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
data class WeightEntryEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val petId: String,
  val date: Long,
  val weightGrams: Int,
  val note: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val deletedAt: Long? = null,
  val syncStatus: String = "LOCAL_ONLY",
)
