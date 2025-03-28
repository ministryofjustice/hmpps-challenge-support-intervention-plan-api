package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.FIRST_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.LAST_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.RESTRICTED_PATIENT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.STATUS_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.SUPPORTING_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipCounts
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referencedata.ReferenceData
import java.time.LocalDate
import java.util.UUID

@Immutable
@Entity
@Table(name = "csip_summary")
class CsipSummary(

  val prisonNumber: String,
  val firstName: String,
  val lastName: String,
  val restrictedPatient: Boolean,
  val prisonCode: String?,
  val cellLocation: String?,
  val supportingPrisonCode: String?,

  val logCode: String,
  val referralDate: LocalDate,
  val incidentType: String,
  val nextReviewDate: LocalDate?,
  val caseManager: String?,
  @Enumerated(EnumType.STRING)
  val statusCode: CsipStatus,
  val statusDescription: String,
  val priority: Int,
  val closedDate: LocalDate?,

  @Id
  val id: UUID,
) {
  companion object {
    val ID: String = CsipSummary::id.name
    val PRISON_NUMBER: String = CsipSummary::prisonNumber.name
    val FIRST_NAME: String = CsipSummary::firstName.name
    val LAST_NAME: String = CsipSummary::lastName.name
    val RESTRICTED_PATIENT: String = CsipSummary::restrictedPatient.name
    val PRISON_CODE: String = CsipSummary::prisonCode.name
    val SUPPORTING_PRISON_CODE: String = CsipSummary::supportingPrisonCode.name
    val CELL_LOCATION: String = CsipSummary::cellLocation.name
    val STATUS_CODE: String = CsipSummary::statusCode.name
    val REFERRAL_DATE: String = CsipSummary::referralDate.name
    val STATUS_DESCRIPTION: String = CsipSummary::statusDescription.name
  }
}

interface CsipSummaryRepository :
  JpaRepository<CsipSummary, UUID>,
  JpaSpecificationExecutor<CsipSummary> {

  @Query(
    """
    with counts as (
        select  csip.prisonNumber                                               as prisonNumber, 
                sum(case when p.id is not null then 1 else 0 end)               as op,
                sum(case when ref.referralComplete = true then 1 else 0 end)    as re
        from CsipRecord csip
        join csip.referral ref
        left join csip.plan p
        where csip.prisonNumber = :prisonNumber
        group by csip.prisonNumber
    )
    select cur as current, c.op as opened, c.re as referred from CsipSummary cur
    join counts c on c.prisonNumber = cur.prisonNumber
    where cur.prisonNumber = :prisonNumber
    order by cur.priority, cur.referralDate desc, cur.id desc
    limit 1
  """,
  )
  fun findCurrentWithCounts(prisonNumber: String): CurrentCsipAndCounts?

  @Query(
    """
    select new uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipCounts(
           sum(case when csip.statusCode = 'REFERRAL_SUBMITTED' then 1 else 0 end)                                  as submittedReferrals,
           sum(case when csip.statusCode = 'INVESTIGATION_PENDING' then 1 else 0 end)                               as pendingInvestigations,
           sum(case when csip.statusCode = 'AWAITING_DECISION' then 1 else 0 end)                                   as awaitingDecisions,
           sum(case when csip.statusCode = 'PLAN_PENDING' then 1 else 0 end)                                        as pendingPlans,
           sum(case when csip.statusCode = 'CSIP_OPEN' then 1 else 0 end)                                           as open,
           sum(case when csip.statusCode = 'CSIP_OPEN' and csip.nextReviewDate < current_date then 1 else 0 end)    as overdueReviews
    )
    from CsipSummary csip
    where csip.prisonCode = :prisonCode
    group by csip.prisonCode
  """,
  )
  fun getOverviewCounts(prisonCode: String): CsipCounts?
}

interface CurrentCsipAndCounts {
  val current: CsipSummary
  val opened: Int
  val referred: Int
}

fun summaryPrisonInvolvement(prisonCodes: Set<String>) = Specification<CsipSummary> { csip, _, cb ->
  cb.or(csip.get<String>(PRISON_CODE).`in`(prisonCodes), csip.get<String>(SUPPORTING_PRISON_CODE).`in`(prisonCodes))
}

fun summaryWithoutRestrictedPatients() = Specification<CsipSummary> { csip, _, cb -> cb.equal(csip.get<Boolean>(RESTRICTED_PATIENT), false) }

fun summaryMatchesPrisonNumber(prisonNumber: String) = Specification<CsipSummary> { csip, _, cb -> cb.equal(cb.lower(csip[PRISON_NUMBER]), prisonNumber.lowercase()) }

fun summaryMatchesName(name: String) = Specification<CsipSummary> { csip, _, cb ->
  val matches = name.split("\\s".toRegex()).map {
    cb.or(
      cb.like(cb.lower(csip[LAST_NAME]), "%${it.lowercase()}%", '\\'),
      cb.like(cb.lower(csip[FIRST_NAME]), "%${it.lowercase()}%", '\\'),
    )
  }.toTypedArray()
  cb.and(*matches)
}

fun summaryHasStatus(statuses: Set<CsipStatus>) = Specification<CsipSummary> { csip, _, cb ->
  csip.get<String>(STATUS_CODE).`in`(statuses.map { it.name })
}

fun CsipSummary.status() = ReferenceData(statusCode.name, statusDescription)
