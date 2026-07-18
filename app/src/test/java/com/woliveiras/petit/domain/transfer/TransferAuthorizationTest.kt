package com.woliveiras.petit.domain.transfer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransferAuthorizationTest {
  @Test
  fun connectedButNotAuthorizedEndpointCannotSendData() {
    assertThat(
        TransferAuthorization.acceptsData(
          connectionEndpointId = "peer",
          authorizedEndpointId = null,
          sourceEndpointId = "peer",
        )
      )
      .isFalse()
  }

  @Test
  fun onlyTheAuthorizedConnectedEndpointCanSendData() {
    assertThat(TransferAuthorization.acceptsData("peer", "peer", "peer")).isTrue()
    assertThat(TransferAuthorization.acceptsData("peer", "peer", "attacker")).isFalse()
  }
}
