package com.woliveiras.petit.domain.model

/** Pet species/type. */
enum class PetType(val displayName: String) {
  CAT("Cat"),
  DOG("Dog"),
  RABBIT("Rabbit"),
  BIRD("Bird"),
  HAMSTER("Hamster"),
  OTHER("Other"),
}

/** Pet sex/gender. */
enum class Sex {
  MALE,
  FEMALE,
  UNKNOWN,
}

/** Synchronization status for local-first architecture. */
enum class SyncStatus {
  LOCAL_ONLY,
  PENDING_SYNC,
  SYNCED,
  CONFLICT,
}

/**
 * Types of vaccines. [applicablePetTypes] = empty set means the vaccine applies to ALL pet types.
 * Non-empty set means the vaccine only applies to those specific pet types.
 */
enum class VaccineType(
  val displayName: String,
  val defaultIntervalMonths: Int?,
  val applicablePetTypes: Set<PetType> = emptySet(),
) {
  // All pets
  RABIES("Antirrábica / Rabies", 12, emptySet()),
  OTHER("Outra / Other", null, emptySet()),

  // Cats only
  V3("V3 - Trivalente", 12, setOf(PetType.CAT)),
  V4("V4 - Tetravalente", 12, setOf(PetType.CAT)),
  V5("V5 - Pentavalente", 12, setOf(PetType.CAT)),
  FELV("Leucemia Felina (FeLV)", 12, setOf(PetType.CAT)),
  FIV("Imunodeficiência Felina (FIV)", 12, setOf(PetType.CAT)),

  // Dogs only
  DHPP("DHPP - Quádrupla/Óctupla Canina", 12, setOf(PetType.DOG)),
  BORDETELLA("Bordetella / Tosse dos Canis", 6, setOf(PetType.DOG)),
  LEPTOSPIROSIS("Leptospirose", 12, setOf(PetType.DOG)),
  LEISHMANIA("Leishmaniose", 12, setOf(PetType.DOG)),
  GRIPE_CANINA("Gripe Canina", 12, setOf(PetType.DOG)),

  // Rabbits only
  RHDV("RHDV - Calicivirose", 12, setOf(PetType.RABBIT)),
  MYXOMATOSIS("Mixomatose", 12, setOf(PetType.RABBIT)),

  // Birds only
  POLYOMAVIRUS("Poliomavírus Aviário", 12, setOf(PetType.BIRD));

  companion object {
    /** Returns vaccine types applicable to the given pet type. */
    fun forPetType(petType: PetType): List<VaccineType> =
      entries.filter { it.applicablePetTypes.isEmpty() || petType in it.applicablePetTypes }
  }
}

/** Status of vaccination or deworming. */
enum class HealthStatus(val displayName: String) {
  OK("Em dia"),
  SCHEDULED("Agendado"),
  OVERDUE("Atrasado"),
}

/** Types of deworming treatments. */
enum class DewormingType(val displayName: String, val defaultIntervalMonths: Int) {
  INTERNAL("Vermífugo interno", 4),
  EXTERNAL("Antipulgas/carrapatos", 1),
  BOTH("Combo (interno + externo)", 3),
}

/** Types of tasks. */
enum class TaskKind {
  WEIGHT,
  VACCINATION,
  DEWORMING,
  MEDICATION,
  CUSTOM,
}

/** Task status. */
enum class TaskStatus {
  PENDING,
  COMPLETED,
}
