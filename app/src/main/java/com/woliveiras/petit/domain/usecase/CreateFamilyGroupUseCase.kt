package com.woliveiras.petit.domain.usecase

import com.woliveiras.petit.data.repository.FamilyGroupRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Use case that creates a new family group and registers the local device as the first member. */
@Singleton
class CreateFamilyGroupUseCase
@Inject
constructor(private val familyGroupRepository: FamilyGroupRepository) {

  /** Creates a family group and returns the family group key. */
  suspend operator fun invoke(deviceName: String): String {
    return familyGroupRepository.createFamilyGroup(deviceName)
  }
}
