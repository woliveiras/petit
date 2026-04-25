package com.woliveiras.petit.domain.usecase

import com.woliveiras.petit.data.repository.NearbyTransferRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Use case that serializes local data into an ExportBundle and sends it via Nearby Connections. */
@Singleton
class SendDataUseCase
@Inject
constructor(
  private val exportImportUseCase: ExportImportUseCase,
  private val nearbyTransferRepository: NearbyTransferRepository,
) {

  /** Exports all local data and sends it to the connected device. */
  suspend operator fun invoke(endpointId: String) {
    val bundle = exportImportUseCase.exportAll()
    nearbyTransferRepository.sendData(endpointId, bundle)
  }
}
