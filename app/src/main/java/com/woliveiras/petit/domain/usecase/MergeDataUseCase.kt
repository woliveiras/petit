package com.woliveiras.petit.domain.usecase

import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.domain.model.ConflictResolution
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.MergeResult
import com.woliveiras.petit.domain.model.SyncLog
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Use case that merges a received ExportBundle into local data using the existing import logic. */
@Singleton
class MergeDataUseCase
@Inject
constructor(
  private val exportImportUseCase: ExportImportUseCase,
  private val familyGroupRepository: FamilyGroupRepository,
) {

  /**
   * Merges the received bundle with local data using last-write-wins by updatedAt.
   *
   * @param bundle The data received from the remote device.
   * @param peerId The ID of the peer that sent the data.
   * @param peerName The display name of the peer device.
   * @param replace If true, replaces all local data; if false, merges by updatedAt.
   * @return The result of the merge operation.
   */
  suspend operator fun invoke(
    bundle: ExportBundle,
    peerId: String,
    peerName: String,
    replace: Boolean = false,
  ): MergeResult {
    val resolution = if (replace) ConflictResolution.REPLACE else ConflictResolution.MERGE

    val result = exportImportUseCase.importData(bundle, resolution)

    val syncLog =
      SyncLog(
        id = UUID.randomUUID().toString(),
        peerId = peerId,
        peerName = peerName,
        syncTimestamp = System.currentTimeMillis(),
        entitiesSent = 0,
        entitiesReceived = result.totalAdded + result.totalUpdated,
        conflictsResolved = result.conflictsResolved,
        syncType = if (replace) "REPLACE" else "MERGE",
      )
    familyGroupRepository.recordSyncLog(syncLog)

    return result
  }
}
