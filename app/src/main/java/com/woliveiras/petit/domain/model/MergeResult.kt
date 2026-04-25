package com.woliveiras.petit.domain.model

/** Result of merging data from a remote device with local data. */
data class MergeResult(
  val petsAdded: Int = 0,
  val petsUpdated: Int = 0,
  val weightsAdded: Int = 0,
  val weightsUpdated: Int = 0,
  val vaccinationsAdded: Int = 0,
  val vaccinationsUpdated: Int = 0,
  val dewormingsAdded: Int = 0,
  val dewormingsUpdated: Int = 0,
  val tasksAdded: Int = 0,
  val tasksUpdated: Int = 0,
  val conflictsResolved: Int = 0,
) {
  val totalAdded: Int
    get() = petsAdded + weightsAdded + vaccinationsAdded + dewormingsAdded + tasksAdded

  val totalUpdated: Int
    get() = petsUpdated + weightsUpdated + vaccinationsUpdated + dewormingsUpdated + tasksUpdated
}
