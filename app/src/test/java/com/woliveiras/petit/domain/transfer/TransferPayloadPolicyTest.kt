package com.woliveiras.petit.domain.transfer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransferPayloadPolicyTest {
  @Test
  fun bytesAreUsedAtOrBelowTheNearbyBytesLimit() {
    assertThat(TransferPayloadPolicy.select(TransferPayloadPolicy.MAX_BYTES_PAYLOAD_SIZE))
      .isEqualTo(TransferPayloadMode.Bytes)
  }

  @Test
  fun fileIsUsedAboveTheNearbyBytesLimit() {
    assertThat(TransferPayloadPolicy.select(TransferPayloadPolicy.MAX_BYTES_PAYLOAD_SIZE + 1))
      .isEqualTo(TransferPayloadMode.File)
  }

  @Test
  fun payloadAboveTheApplicationLimitIsRejected() {
    val result = runCatching {
      TransferPayloadPolicy.select(TransferPayloadPolicy.MAX_TRANSFER_SIZE + 1)
    }

    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun progressNeverMovesBackwardOrBeyondTotalBytes() {
    val progress = MonotonicTransferProgress(totalBytes = 100)

    assertThat(progress.update(40)).isEqualTo(40)
    assertThat(progress.update(20)).isEqualTo(40)
    assertThat(progress.update(120)).isEqualTo(100)
  }
}
