package com.woliveiras.petit.presentation.feature.familygroup

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.data.repository.NearbyTransferRepository
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.PairingState
import com.woliveiras.petit.domain.usecase.CreateFamilyGroupUseCase
import com.woliveiras.petit.domain.usecase.JoinFamilyGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PairingUiState(
  val pairingState: PairingState = PairingState.Idle,
  val isCreatingGroup: Boolean = true,
  val familyGroupKey: String? = null,
)

@HiltViewModel
class PairingViewModel
@Inject
constructor(
  private val nearbyTransferRepository: NearbyTransferRepository,
  private val familyGroupRepository: FamilyGroupRepository,
  private val createFamilyGroupUseCase: CreateFamilyGroupUseCase,
  private val joinFamilyGroupUseCase: JoinFamilyGroupUseCase,
) : ViewModel() {

  private val _uiState = MutableStateFlow(PairingUiState())
  val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

  init {
    observePairingState()
  }

  private fun observePairingState() {
    viewModelScope.launch {
      nearbyTransferRepository.pairingState.collect { state ->
        _uiState.update { it.copy(pairingState = state) }

        if (state is PairingState.Paired) {
          onPairingComplete(state)
        }
      }
    }
  }

  fun startAdvertising() {
    viewModelScope.launch {
      _uiState.update { it.copy(isCreatingGroup = true) }
      val deviceName = Build.MODEL
      val key = createFamilyGroupUseCase(deviceName)
      _uiState.update { it.copy(familyGroupKey = key) }
      nearbyTransferRepository.startAdvertising(deviceName, key)
    }
  }

  fun startDiscovery() {
    viewModelScope.launch {
      _uiState.update { it.copy(isCreatingGroup = false) }
      // Discovery doesn't require a group key — the advertiser sends it after connection
      nearbyTransferRepository.startDiscovery("")
    }
  }

  fun acceptConnection(endpointId: String) {
    viewModelScope.launch { nearbyTransferRepository.acceptConnection(endpointId) }
  }

  fun rejectConnection(endpointId: String) {
    viewModelScope.launch { nearbyTransferRepository.rejectConnection(endpointId) }
  }

  fun cancel() {
    nearbyTransferRepository.stopAdvertising()
    nearbyTransferRepository.stopDiscovery()
    nearbyTransferRepository.disconnect()
    _uiState.update { it.copy(pairingState = PairingState.Idle) }
  }

  fun onPermissionDenied() {
    _uiState.update {
      it.copy(pairingState = PairingState.Error("Nearby permissions are required to pair devices"))
    }
  }

  private suspend fun onPairingComplete(state: PairingState.Paired) {
    val familyGroupKey = state.familyGroupKey
    if (familyGroupKey.isBlank()) return

    if (!_uiState.value.isCreatingGroup) {
      // Joining device: register in the group with the key received from the advertiser
      joinFamilyGroupUseCase(familyGroupKey, Build.MODEL)
    }
    _uiState.update { it.copy(familyGroupKey = familyGroupKey) }

    // Register the remote device as a member
    val remoteMember =
      FamilyGroupMember(
        id = UUID.randomUUID().toString(),
        deviceName = state.deviceName,
        familyGroupKey = familyGroupKey,
        isLocalDevice = false,
        lastSyncAt = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
      )
    familyGroupRepository.addRemoteMember(remoteMember)
  }

  override fun onCleared() {
    super.onCleared()
    cancel()
  }
}
