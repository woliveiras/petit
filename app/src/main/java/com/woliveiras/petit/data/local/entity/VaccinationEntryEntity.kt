package com.woliveiras.petit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Room entity representing a vaccination entry. */
@Entity(
  tableName = "vaccination_entries",
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
data class VaccinationEntryEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val petId: String,
  val vaccineType: String,
  val customVaccineTypeName: String? = null,
  val applicationDate: Long,
  val nextDueDate: Long? = null,
  val veterinarian: String? = null,
  val clinic: String? = null,
  val batchNumber: String? = null,
  val note: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val deletedAt: Long? = null,
  val syncStatus: String = "LOCAL_ONLY",
)
