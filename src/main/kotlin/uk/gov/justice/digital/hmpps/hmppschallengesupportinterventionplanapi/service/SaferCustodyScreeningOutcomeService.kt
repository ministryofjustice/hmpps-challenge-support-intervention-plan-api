package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.INVESTIGATION_REQUIRED
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpsertSaferCustodyScreeningOutcomeRequest
import java.util.UUID

@Service
@Transactional
class SaferCustodyScreeningOutcomeService(
  private val csipRecordRepository: CsipRecordRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val jdaService: JdaService,
) {

  @PublishCsipEvent(CSIP_UPDATED)
  fun upsertScreeningOutcome(
    recordUuid: UUID,
    request: UpsertSaferCustodyScreeningOutcomeRequest,
  ): SaferCustodyScreeningOutcome {
    val record = verifyCsipRecordExists(csipRecordRepository, recordUuid)

    val referral = verifyExists(record.referral) {
      MissingReferralException(recordUuid)
    }

    val result =
      with(referral) {
        val screening = saferCustodyScreeningOutcome

        upsertSaferCustodyScreeningOutcome(
          request = request,
          referenceDataRepository::getActiveReferenceData,
        ).toModel()
          .apply { new = screening == null }
      }

    if (request.outcomeTypeCode == INVESTIGATION_REQUIRED) {
      record.prisonCodeWhenRecorded?.let { prisonCode ->
        jdaService.submitCaseNotesForAnalysis(
          offenderIdentifier = record.prisonNumber,
          prisonCode = prisonCode,
          correlationId = referral.id.toString(),
        )
      }
    }

    return result
  }
}
