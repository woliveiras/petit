package com.woliveiras.petit.domain.model

/** State machine for data transfer between paired devices. */
sealed interface TransferState {
  /** No transfer in progress. */
  data object Idle : TransferState

  /** Sending data to the remote device. */
  data class Sending(val bytesTransferred: Long, val totalBytes: Long) : TransferState

  /** Receiving data from the remote device. */
  data class Receiving(val bytesTransferred: Long, val totalBytes: Long) : TransferState

  /** Outgoing payload was fully delivered to the authorized peer. */
  data class Sent(val totalBytes: Long) : TransferState

  /** Transfer completed successfully. */
  data class Complete(val bundle: ExportBundle) : TransferState

  /** An error occurred during transfer. */
  data class Error(val reason: TransferError) : TransferState
}

enum class TransferError {
  NoConnectedDevice,
  PayloadTooLarge,
  TransferFailed,
  InvalidData,
  ParseFailed,
  UnsupportedPayload,
  MergeFailed,
}
