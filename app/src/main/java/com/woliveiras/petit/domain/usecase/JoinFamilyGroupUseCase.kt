package com.woliveiras.petit.domain.usecase

import com.woliveiras.petit.data.repository.FamilyGroupRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Use case that joins an existing family group using a pairing code. */
@Singleton
class JoinFamilyGroupUseCase
@Inject
constructor(private val familyGroupRepository: FamilyGroupRepository) {

  /** Joins a family group with the given key. */
  suspend operator fun invoke(familyGroupKey: String, deviceName: String) {
    familyGroupRepository.joinFamilyGroup(familyGroupKey, deviceName)
  }
}
