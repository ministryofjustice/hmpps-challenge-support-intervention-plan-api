package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingReferralException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyCsipRecordExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyExists
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getOutcomeType
import java.util.UUID

@Service
@Transactional
class SaferCustodyScreeningOutcomeService(
  private val csipRecordRepository: CsipRecordRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun createScreeningOutcome(
    recordUuid: UUID,
    request: CreateSaferCustodyScreeningOutcomeRequest,
  ): SaferCustodyScreeningOutcome {
    val outcomeType = referenceDataRepository.getOutcomeType(request.outcomeTypeCode)
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)
    return with(verifyExists(record.referral) { MissingReferralException(recordUuid) }) {
      csipRecordRepository.save(
        this.createSaferCustodyScreeningOutcome(
          context = csipRequestContext(),
          request = request,
          outcomeType = outcomeType,
        ),
      ).referral!!.saferCustodyScreeningOutcome!!.toModel()
    }
  }
}
