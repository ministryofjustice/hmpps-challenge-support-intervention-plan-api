package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummaryRepository

@Transactional
@Service
class PersonReconciliationHandler(
  private val prisonerSearch: PrisonerSearchClient,
  private val psr: PersonSummaryRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun handle(reconciliationEvent: HmppsDomainEvent<PersonReconciliationInformation>) {
    val latestInfo = prisonerSearch.findPrisonerDetails(reconciliationEvent.additionalInformation.prisonNumbers)
      .associateBy { it.prisonerNumber }
    val changes =
      psr.findAllByPrisonNumberIn(reconciliationEvent.additionalInformation.prisonNumbers).mapNotNull { ps ->
        latestInfo[ps.prisonNumber]?.let { prisoner ->
          ps.update(
            prisoner.firstName,
            prisoner.lastName,
            prisoner.status,
            prisoner.restrictedPatient,
            prisoner.prisonId,
            prisoner.cellLocation,
            prisoner.supportingPrisonId,
          )
          val changes = ps.changes()
          if (changes.isEmpty()) {
            null
          } else {
            ps.prisonNumber to changes.joinToString(", ", "[", "]")
          }
        }
      }.toMap()

    if (changes.isNotEmpty()) {
      telemetryClient.trackEvent("PersonReconciliationDiffs", changes, mapOf())
    }
  }
}
