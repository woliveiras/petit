package com.woliveiras.petit.domain.transfer

enum class TransferPayloadMode {
  Bytes,
  File,
}

object TransferPayloadPolicy {
  const val MAX_BYTES_PAYLOAD_SIZE = 1_047_552L
  const val MAX_TRANSFER_SIZE = 10L * 1024 * 1024

  fun select(sizeBytes: Long): TransferPayloadMode {
    require(sizeBytes in 0..MAX_TRANSFER_SIZE) { "Transfer payload exceeds the 10 MB limit" }
    return if (sizeBytes <= MAX_BYTES_PAYLOAD_SIZE) TransferPayloadMode.Bytes
    else TransferPayloadMode.File
  }
}

class MonotonicTransferProgress(private val totalBytes: Long) {
  private var highestBytes = 0L

  init {
    require(totalBytes >= 0)
  }

  @Synchronized
  fun update(bytesTransferred: Long): Long {
    highestBytes = maxOf(highestBytes, bytesTransferred.coerceIn(0, totalBytes))
    return highestBytes
  }
}
