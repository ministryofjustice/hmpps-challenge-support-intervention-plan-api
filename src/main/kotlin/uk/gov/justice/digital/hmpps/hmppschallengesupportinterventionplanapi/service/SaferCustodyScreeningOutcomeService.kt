package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.getActiveReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.PublishCsipEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CreateSaferCustodyScreeningOutcomeRequest
import java.util.UUID

@Service
@Transactional
class SaferCustodyScreeningOutcomeService(
  private val csipRecordRepository: CsipRecordRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  @PublishCsipEvent(CSIP_UPDATED)
  fun createScreeningOutcome(
    recordUuid: UUID,
    request: CreateSaferCustodyScreeningOutcomeRequest,
  ): SaferCustodyScreeningOutcome {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    return with(verifyExists(record.referral) { MissingReferralException(recordUuid) }) {
      createSaferCustodyScreeningOutcome(request = request, referenceDataRepository::getActiveReferenceData).toModel()
    }
  }
}
