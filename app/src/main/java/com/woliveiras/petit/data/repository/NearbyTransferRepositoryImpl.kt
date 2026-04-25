package com.woliveiras.petit.data.repository

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.PairingState
import com.woliveiras.petit.domain.model.TransferState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Implementation of NearbyTransferRepository using Google Nearby Connections API. */
@Singleton
class NearbyTransferRepositoryImpl
@Inject
constructor(@ApplicationContext private val context: Context) : NearbyTransferRepository {

  companion object {
    private const val SERVICE_ID = "com.woliveiras.petit.familygroup"
    private const val KEY_PREFIX = "KEY:"
    private const val MAX_PAYLOAD_SIZE = 10 * 1024 * 1024 // 10 MB
  }

  private val connectionsClient by lazy { Nearby.getConnectionsClient(context) }

  private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
  override val pairingState: Flow<PairingState> = _pairingState.asStateFlow()

  private val _transferState = MutableStateFlow<TransferState>(TransferState.Idle)
  override val transferState: Flow<TransferState> = _transferState.asStateFlow()

  private val _connectedEndpointId = AtomicReference<String?>(null)
  private val _pendingDeviceName = AtomicReference<String?>(null)
  private val advertisedFamilyGroupKey = AtomicReference<String?>(null)
  private val expectedFamilyGroupKey = AtomicReference<String?>(null)

  override val connectedPeerName: String?
    get() = _pendingDeviceName.get()

  override val connectedPeerId: String?
    get() = _connectedEndpointId.get()

  private val receivedPayloadData = StringBuffer()

  private val connectionLifecycleCallback =
    object : ConnectionLifecycleCallback() {
      override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
        _pendingDeviceName.set(info.endpointName)
        _pairingState.value =
          PairingState.ConnectionRequested(deviceName = info.endpointName, endpointId = endpointId)
      }

      override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
        when (result.status.statusCode) {
          ConnectionsStatusCodes.STATUS_OK -> {
            _connectedEndpointId.set(endpointId)
            // If we're the advertiser, send the family group key to the discoverer
            val key = advertisedFamilyGroupKey.get()
            if (key != null) {
              val keyPayload = Payload.fromBytes("$KEY_PREFIX$key".toByteArray())
              connectionsClient.sendPayload(endpointId, keyPayload)
              _pairingState.value =
                PairingState.Paired(
                  familyGroupKey = key,
                  deviceName = _pendingDeviceName.get() ?: endpointId,
                )
            }
            // If discoverer, we wait for key payload in payloadCallback
          }
          else -> {
            _pairingState.value = PairingState.Error("Connection failed")
          }
        }
      }

      override fun onDisconnected(endpointId: String) {
        _connectedEndpointId.set(null)
        _pairingState.value = PairingState.Idle
        _transferState.value = TransferState.Idle
      }
    }

  private val payloadCallback =
    object : PayloadCallback() {
      override fun onPayloadReceived(endpointId: String, payload: Payload) {
        if (payload.type == Payload.Type.BYTES) {
          payload.asBytes()?.let { bytes ->
            val data = String(bytes)
            // Check if this is a key exchange payload
            if (data.startsWith(KEY_PREFIX)) {
              val receivedKey = data.removePrefix(KEY_PREFIX)
              // Validate the received key matches what we expect (discoverer side)
              val expected = expectedFamilyGroupKey.get()
              if (expected != null && receivedKey != expected) {
                _pairingState.value = PairingState.Error("Key mismatch — wrong family group")
                _connectedEndpointId.get()?.let { connectionsClient.disconnectFromEndpoint(it) }
                return
              }
              _pairingState.value =
                PairingState.Paired(
                  familyGroupKey = receivedKey,
                  deviceName = _pendingDeviceName.get() ?: endpointId,
                )
            } else {
              // Guard against unbounded payload accumulation
              if (receivedPayloadData.length + data.length > MAX_PAYLOAD_SIZE) {
                receivedPayloadData.delete(0, receivedPayloadData.length)
                _transferState.value = TransferState.Error("Received payload too large")
                return
              }
              receivedPayloadData.append(data)
            }
          }
        }
      }

      override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
        when (update.status) {
          PayloadTransferUpdate.Status.IN_PROGRESS -> {
            if (receivedPayloadData.isNotEmpty()) {
              _transferState.value =
                TransferState.Receiving(
                  bytesTransferred = update.bytesTransferred,
                  totalBytes = update.totalBytes,
                )
            }
          }
          PayloadTransferUpdate.Status.SUCCESS -> {
            val data = receivedPayloadData.toString()
            if (data.isNotEmpty()) {
              try {
                val json = JSONObject(data)
                val bundle = ExportBundle.fromJson(json)
                val errors = ExportBundle.validate(bundle)
                if (errors.isNotEmpty()) {
                  _transferState.value = TransferState.Error("Invalid data received from peer")
                } else {
                  _transferState.value = TransferState.Complete(bundle)
                }
              } catch (_: Exception) {
                _transferState.value = TransferState.Error("Failed to parse received data")
              } finally {
                receivedPayloadData.delete(0, receivedPayloadData.length)
              }
            }
          }
          PayloadTransferUpdate.Status.FAILURE -> {
            receivedPayloadData.delete(0, receivedPayloadData.length)
            _transferState.value = TransferState.Error("Transfer failed")
          }
          PayloadTransferUpdate.Status.CANCELED -> {
            receivedPayloadData.delete(0, receivedPayloadData.length)
            _transferState.value = TransferState.Idle
          }
        }
      }
    }

  private val endpointDiscoveryCallback =
    object : EndpointDiscoveryCallback() {
      override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
        connectionsClient.requestConnection(
          android.os.Build.MODEL,
          endpointId,
          connectionLifecycleCallback,
        )
      }

      override fun onEndpointLost(endpointId: String) {
        // Endpoint lost during discovery — no action needed
      }
    }

  override suspend fun startAdvertising(deviceName: String, familyGroupKey: String) {
    advertisedFamilyGroupKey.set(familyGroupKey)
    _pairingState.value = PairingState.WaitingForConnection(code = familyGroupKey.take(4))
    val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
    connectionsClient.startAdvertising(deviceName, SERVICE_ID, connectionLifecycleCallback, options)
  }

  override fun stopAdvertising() {
    connectionsClient.stopAdvertising()
    advertisedFamilyGroupKey.set(null)
    if (_pairingState.value is PairingState.WaitingForConnection) {
      _pairingState.value = PairingState.Idle
    }
  }

  override suspend fun startDiscovery(familyGroupKey: String) {
    expectedFamilyGroupKey.set(familyGroupKey)
    _pairingState.value = PairingState.WaitingForConnection(code = "")
    val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
    connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
  }

  override fun stopDiscovery() {
    connectionsClient.stopDiscovery()
  }

  override suspend fun acceptConnection(endpointId: String) {
    connectionsClient.acceptConnection(endpointId, payloadCallback)
  }

  override suspend fun rejectConnection(endpointId: String) {
    connectionsClient.rejectConnection(endpointId)
    _pairingState.value = PairingState.Idle
  }

  override suspend fun sendData(endpointId: String, bundle: ExportBundle) {
    val json = bundle.toJson().toString()
    val bytes = json.toByteArray()
    _transferState.value =
      TransferState.Sending(bytesTransferred = 0, totalBytes = bytes.size.toLong())
    val payload = Payload.fromBytes(bytes)
    connectionsClient.sendPayload(endpointId, payload)
  }

  override fun disconnect() {
    _connectedEndpointId.get()?.let { connectionsClient.disconnectFromEndpoint(it) }
    _connectedEndpointId.set(null)
    advertisedFamilyGroupKey.set(null)
    expectedFamilyGroupKey.set(null)
    _pendingDeviceName.set(null)
    receivedPayloadData.delete(0, receivedPayloadData.length)
  }
}
