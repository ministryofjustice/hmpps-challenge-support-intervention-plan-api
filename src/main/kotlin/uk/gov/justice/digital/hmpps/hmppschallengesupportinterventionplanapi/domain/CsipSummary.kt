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

  val referralDate: LocalDate,
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
    with counts as (select csip.prison_number                                        as prisonNumber,
                       sum(case when p.plan_id is not null then 1 else 0 end)        as op,
                       sum(case when ref.referral_complete = true then 1 else 0 end) as re
                from csip_record csip
                         join referral ref on ref.referral_id = csip.record_id
                         left join plan p on p.plan_id = csip.record_id
                where csip.prison_number = :prisonNumber
                group by csip.prison_number),
         latest_review as (select distinct on (r.plan_id) r.plan_id,
                                                          r.review_id,
                                                          r.next_review_date,
                                                          r.csip_closed_date
                       from review r
                                join csip_record csip on csip.record_id = r.plan_id
                       where csip.prison_number = :prisonNumber
                       order by r.plan_id, r.review_sequence desc)
    select csip.record_id                        as id,
           ref.referral_date                     as referralDate,
           case
               when rev.review_id is not null then rev.next_review_date
               else p.first_case_review_date end as nextReviewDate,
           rev.csip_closed_date                  as closedDate,
           status.code                           as statusCode,
           status.description                    as statusDescription,
           case
               when status.code = 'CSIP_OPEN' then 1
               when status.code = 'CSIP_CLOSED' then 3
               when status.code in ('NO_FURTHER_ACTION', 'SUPPORT_OUTSIDE_CSIP') then 4
               else 2
               end                               as priority,
           counts.op                             as opened,
           counts.re                             as referred
    from csip_record csip
             join reference_data status on status.reference_data_id = csip.status_id
             join referral ref on ref.referral_id = csip.record_id
             left join plan p on p.plan_id = csip.record_id
             left join latest_review rev on rev.plan_id = p.plan_id
             join counts on csip.prison_number = counts.prisonNumber
    where csip.prison_number = :prisonNumber
    order by priority, referral_date desc, id desc
    limit 1
  """,
    nativeQuery = true,
  )
  fun findCurrentWithCounts(prisonNumber: String): CurrentCsipAndCounts?

  @Query(
    """
    with latest_review as (select distinct on (r.plan_id) r.plan_id,
                                                          r.review_id,
                                                          r.next_review_date,
                                                          r.csip_closed_date
                       from review r
                                join csip_record csip on csip.record_id = r.plan_id
                                join person_summary ps on ps.prison_number = csip.prison_number
                       where ps.prison_code = :prisonCode
                       order by r.plan_id, r.review_sequence desc)
    select sum(case when status.code = 'REFERRAL_SUBMITTED' then 1 else 0 end)      as submittedReferrals,
           sum(case when status.code = 'INVESTIGATION_PENDING' then 1 else 0 end)   as pendingInvestigations,
           sum(case when status.code = 'AWAITING_DECISION' then 1 else 0 end)       as awaitingDecisions,
           sum(case when status.code = 'PLAN_PENDING' then 1 else 0 end)            as pendingPlans,
           sum(case when status.code = 'CSIP_OPEN' then 1 else 0 end)               as open,
           sum(case
                   when status.code = 'CSIP_OPEN' and coalesce(lr.next_review_date, p.first_case_review_date) < current_date
                       then 1
                   else 0 end)                                                      as overdueReviews
    from csip_record csip
             join person_summary ps on ps.prison_number = csip.prison_number
             join reference_data status on status.reference_data_id = csip.status_id
             left join plan p on p.plan_id = csip.record_id
             left join latest_review lr on lr.plan_id = p.plan_id
    where ps.prison_code = :prisonCode
    group by ps.prison_code
  """,
    nativeQuery = true,
  )
  fun getOverviewCounts(prisonCode: String): CsipCounts?
}

interface CurrentCsipAndCounts {
  val id: UUID
  val referralDate: LocalDate
  val nextReviewDate: LocalDate?
  val closedDate: LocalDate?
  val statusCode: CsipStatus
  val statusDescription: String
  val priority: Int
  val opened: Int
  val referred: Int
}

fun summaryMatchesPrison(prisonNumber: String) = Specification<CsipSummary> { csip, _, cb -> cb.equal(cb.lower(csip[PRISON_CODE]), prisonNumber.lowercase()) }

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

fun summaryHasStatus(status: CsipStatus) = Specification<CsipSummary> { csip, _, cb ->
  cb.equal(csip.get<String>(STATUS_CODE), status.name)
}

fun CsipSummary.status() = ReferenceData(statusCode.name, statusDescription)
