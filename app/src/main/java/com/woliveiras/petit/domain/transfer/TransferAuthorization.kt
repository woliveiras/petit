package com.woliveiras.petit.domain.transfer

object TransferAuthorization {
  fun acceptsData(
    connectionEndpointId: String?,
    authorizedEndpointId: String?,
    sourceEndpointId: String,
  ): Boolean = connectionEndpointId == sourceEndpointId && authorizedEndpointId == sourceEndpointId
}
