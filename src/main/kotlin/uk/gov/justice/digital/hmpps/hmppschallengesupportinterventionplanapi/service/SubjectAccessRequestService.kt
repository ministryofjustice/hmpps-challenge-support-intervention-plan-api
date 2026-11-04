package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.sar.SarCsipRecord
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

/**
 * Serves every CSIP a prisoner has had, for a subject access request.
 *
 * Declaring this bean is the whole wiring: hmpps-kotlin-spring-boot-starter registers `GET /subject-access-request`
 * because a [HmppsPrisonSubjectAccessRequestService] is present, and guards it with the SAR_DATA_ACCESS role. The
 * report template is served from `GET /subject-access-request/template`, configured under `hmpps.sar.template`.
 *
 * Every CSIP is returned whatever its status, including referrals that ended at screening with no further action,
 * and CSIPs first recorded in NOMIS. A CSIP belongs to the prison number it is recorded against now, so after a
 * prisoner merge it is found under the number it was moved to.
 */
@Service
class SubjectAccessRequestService(
  private val csipRecordRepository: CsipRecordRepository,
) : HmppsPrisonSubjectAccessRequestService {

  /**
   * [fromDate] and [toDate] are both inclusive. A CSIP is returned in full if any part of it falls within them, so
   * a prisoner never receives part of a CSIP - as agreed with the Offender SAR team at the data review.
   *
   * A CSIP runs from the earliest date recorded on it to the latest, or to today while it is still open, so an open
   * CSIP is not returned for dates wholly in the future. The dates are those of things that happened - the incident,
   * referral, screening, interviews, decision, needs being opened and closed, reviews and closure - not dates planned
   * for the future, such as a target or next review date.
   *
   * A prisoner has few CSIPs, so they are filtered here rather than in the query, where the same rule would need a
   * join to every part of the CSIP. Newest referral first. Returns null when there are none, which the library
   * controller turns into a 204.
   */
  @Transactional(readOnly = true)
  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val records = csipRecordRepository.findByPrisonNumber(prn)
      .filter { it.overlaps(fromDate, toDate) }
      .sortedWith(compareByDescending<CsipRecord> { it.referral?.referralDate }.thenByDescending { it.id })
    if (records.isEmpty()) {
      return null
    }
    return HmppsSubjectAccessRequestContent(content = records.map(SarCsipRecord::from))
  }

  private fun CsipRecord.overlaps(from: LocalDate?, to: LocalDate?): Boolean {
    val dates = datesOfActivity()
    val start = dates.minOrNull() ?: return true
    val latest = dates.max()
    val end = if (isOpen()) maxOf(latest, LocalDate.now()) else latest
    return (to == null || !start.isAfter(to)) && (from == null || !end.isBefore(from))
  }

  private fun CsipRecord.isOpen() = status?.code?.let { it !in FINISHED } ?: true

  private fun CsipRecord.datesOfActivity(): List<LocalDate> = buildList {
    referral?.run {
      add(incidentDate)
      add(referralDate)
      referralCompletedDate?.let(::add)
      saferCustodyScreeningOutcome?.let { add(it.date) }
      investigation?.interviews()?.forEach { add(it.interviewDate) }
      decisionAndActions?.date?.let(::add)
    }
    plan?.run {
      identifiedNeeds().forEach { need ->
        add(need.createdDate)
        need.closedDate?.let(::add)
      }
      reviews().forEach { review ->
        add(review.reviewDate)
        review.csipClosedDate?.let(::add)
      }
      closedDate?.let(::add)
    }
  }

  private companion object {
    /** Statuses after which nothing more happens on the CSIP. Anything else, including unknown, counts as open. */
    val FINISHED = setOf(
      CsipStatus.CSIP_CLOSED,
      CsipStatus.NO_FURTHER_ACTION,
      CsipStatus.SUPPORT_OUTSIDE_CSIP,
      CsipStatus.ACCT_SUPPORT,
    ).map { it.name }.toSet()
  }
}
