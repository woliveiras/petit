package com.woliveiras.petit.domain.usecase

import com.woliveiras.petit.data.repository.DewormingEntryRepository
import com.woliveiras.petit.data.repository.VaccinationEntryRepository
import com.woliveiras.petit.data.repository.WeightEntryRepository
import com.woliveiras.petit.domain.model.HealthStatus
import com.woliveiras.petit.domain.model.PetHealthSummary
import com.woliveiras.petit.domain.model.WeightEntry
import com.woliveiras.petit.domain.model.WeightStatus
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Fetches and computes the aggregated health summary for a given pet. */
interface GetPetHealthSummaryAction {
  suspend fun execute(petId: String): PetHealthSummary
}

@Singleton
class GetPetHealthSummaryUseCase
@Inject
constructor(
  private val weightEntryRepository: WeightEntryRepository,
  private val vaccinationEntryRepository: VaccinationEntryRepository,
  private val dewormingEntryRepository: DewormingEntryRepository,
  private val clock: Clock,
) : GetPetHealthSummaryAction {

  override suspend fun execute(petId: String): PetHealthSummary {
    val latestWeight = weightEntryRepository.getLatestWeightEntry(petId)
    val vaccinations = vaccinationEntryRepository.getLatestVaccinationsForPet(petId).first()
    val dewormings = dewormingEntryRepository.getLatestDewormingsForPet(petId).first()

    val vaccinationStatus = getWorstStatus(vaccinations.map { it.status })
    val dewormingStatus = getWorstStatus(dewormings.map { it.status })
    val overallStatus = getWorstStatus(listOf(vaccinationStatus, dewormingStatus))
    val weightStatus = calculateWeightStatus(latestWeight)

    val nextVaccination =
      vaccinations.filter { it.nextDueDate != null }.minByOrNull { it.nextDueDate!! }
    val nextDeworming =
      dewormings.filter { it.nextDueDate != null }.minByOrNull { it.nextDueDate!! }

    return PetHealthSummary(
      petId = petId,
      latestWeight = latestWeight,
      weightStatus = weightStatus,
      overallStatus = overallStatus,
      nextVaccineType = nextVaccination?.vaccineType,
      nextVaccinationDate = nextVaccination?.nextDueDate,
      nextDewormingType = nextDeworming?.type,
      nextDewormingDate = nextDeworming?.nextDueDate,
    )
  }

  private fun calculateWeightStatus(latestWeight: WeightEntry?): WeightStatus {
    if (latestWeight == null) return WeightStatus.NO_DATA

    val daysSinceLastWeight = ChronoUnit.DAYS.between(latestWeight.date, LocalDate.now(clock))
    return if (daysSinceLastWeight <= 15) WeightStatus.UPDATED else WeightStatus.OUTDATED
  }

  private fun getWorstStatus(statuses: List<HealthStatus>): HealthStatus {
    return statuses.maxByOrNull { status ->
      when (status) {
        HealthStatus.OVERDUE -> 2
        HealthStatus.SCHEDULED -> 1
        HealthStatus.OK -> 0
      }
    } ?: HealthStatus.OK
  }
}
