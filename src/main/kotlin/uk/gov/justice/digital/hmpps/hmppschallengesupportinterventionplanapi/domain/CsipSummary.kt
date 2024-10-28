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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.STATUS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Immutable
@Entity
@Table(name = "csip_summary")
class CsipSummary(

  val prisonNumber: String,
  val firstName: String,
  val lastName: String,
  val prisonCode: String?,
  val cellLocation: String?,

  val referralDate: LocalDate,
  val nextReviewDate: LocalDate?,
  val caseManager: String?,
  @Enumerated(EnumType.STRING)
  val status: CsipStatus,
  val priority: Int,
  val statusDescription: String,
  val createdAt: LocalDateTime,

  @Id
  val id: UUID,
) {
  companion object {
    val PRISON_NUMBER: String = CsipSummary::prisonNumber.name
    val FIRST_NAME: String = CsipSummary::firstName.name
    val LAST_NAME: String = CsipSummary::lastName.name
    val PRISON_CODE: String = CsipSummary::prisonCode.name
    val CELL_LOCATION: String = CsipSummary::cellLocation.name
    val STATUS: String = CsipSummary::status.name
    val REFERRAL_DATE: String = CsipSummary::referralDate.name
    val CREATED_AT: String = CsipSummary::createdAt.name
    val STATUS_DESCRIPTION: String = CsipSummary::statusDescription.name
  }
}

interface CsipSummaryRepository : JpaRepository<CsipSummary, UUID>, JpaSpecificationExecutor<CsipSummary> {

  @Query(
    """
    with counts as (
        select  csip.prisonNumber prisonNumber, 
                sum(case when p.id is not null then 1 else 0 end) as op,
                sum(case when ref.referralComplete = true then 1 else 0 end) as re
        from CsipRecord csip
        join csip.referral ref
        left join csip.plan p
        where csip.prisonNumber = :prisonNumber
        group by csip.prisonNumber
    )
    select cur as current, c.op as opened, c.re as referred from CsipSummary cur
    join counts c on c.prisonNumber = cur.prisonNumber
    where cur.prisonNumber = :prisonNumber
    order by cur.priority, cur.referralDate desc, cur.createdAt desc
    limit 1
  """,
  )
  fun findCurrentWithCounts(prisonNumber: String): CurrentCsipAndCounts?
}

interface CurrentCsipAndCounts {
  val current: CsipSummary
  val opened: Int
  val referred: Int
}

fun summaryMatchesPrison(prisonNumber: String) =
  Specification<CsipSummary> { csip, _, cb -> cb.equal(cb.lower(csip[PRISON_CODE]), prisonNumber.lowercase()) }

fun summaryMatchesPrisonNumber(prisonNumber: String) =
  Specification<CsipSummary> { csip, _, cb -> cb.equal(cb.lower(csip[PRISON_NUMBER]), prisonNumber.lowercase()) }

fun summaryMatchesName(name: String) =
  Specification<CsipSummary> { csip, _, cb ->
    val matches = name.split("\\s".toRegex()).map {
      cb.or(
        cb.like(cb.lower(csip[LAST_NAME]), "%${it.lowercase()}%"),
        cb.like(cb.lower(csip[FIRST_NAME]), "%${it.lowercase()}%"),
      )
    }.toTypedArray()
    cb.and(*matches)
  }

fun summaryHasStatus(status: CsipStatus) = Specification<CsipSummary> { csip, _, cb ->
  cb.equal(csip.get<String>(STATUS), status.name)
}
