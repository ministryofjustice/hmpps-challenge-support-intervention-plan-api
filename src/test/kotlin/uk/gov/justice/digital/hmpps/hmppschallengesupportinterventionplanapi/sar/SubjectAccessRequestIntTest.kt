package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sar

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.repository.findByIdOrNull
import tools.jackson.databind.JsonNode
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.SCREENING_OUTCOME_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.NomisIdGenerator.prisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
import java.time.LocalDate

/**
 * Which CSIPs a subject access request returns. The content of each is covered by [SubjectAccessRequestLibraryTest].
 *
 * The date range rule agreed with the Offender SAR team: a CSIP is returned in full if any part of it falls within
 * the requested dates. Each test here uses a prisoner of its own, so the CSIPs of other tests cannot interfere.
 */
class SubjectAccessRequestIntTest : IntegrationTestBase() {

  @Test
  fun `requires the SAR_DATA_ACCESS role`() {
    webTestClient.get().uri("/subject-access-request?prn=${prisonNumber()}")
      .headers(setAuthorisation(roles = listOf("ROLE_CSIP_UI")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `204 when the prisoner has no CSIPs`() {
    webTestClient.get().uri("/subject-access-request?prn=${prisonNumber()}")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isNoContent
  }

  @Test
  fun `returns every CSIP the prisoner has had, newest referral first, when no dates are given`() {
    val prn = prisonNumber()
    givenClosedCsip(prn, logCode = "OLDER", referral = "2024-01-10", closed = "2024-03-01")
    givenOpenCsip(prn, logCode = "NEWER", referral = "2025-02-01")
    givenOpenCsip(prisonNumber(), logCode = "OTHER", referral = "2025-02-01")

    assertThat(logCodes(prn)).containsExactly("NEWER", "OLDER")
  }

  @ParameterizedTest(name = "{0} to {1}: {2}")
  @CsvSource(
    // A closed CSIP running from 10 January (incident 8 January) to 1 March 2024
    "2023-01-01, 2024-01-07, ''",
    "2023-01-01, 2024-01-08, CLOSED",
    "2024-02-01, 2024-02-02, CLOSED",
    "2024-03-01, 2024-12-31, CLOSED",
    "2024-03-02, 2024-12-31, ''",
    // Either end may be left open
    ", 2024-01-07, ''",
    "2024-03-02, , OPEN",
  )
  fun `returns a CSIP in full if any part of it falls within the dates`(from: String?, to: String?, expected: String?) {
    val prn = prisonNumber()
    givenClosedCsip(prn, logCode = "CLOSED", referral = "2024-01-10", closed = "2024-03-01")
    givenOpenCsip(prn, logCode = "OPEN", referral = "2025-02-01")

    val codes = logCodes(prn, from, to)

    assertThat(codes).containsExactlyElementsOf(expected?.takeIf { it.isNotBlank() }?.split(" ").orEmpty())
  }

  @Test
  fun `an open CSIP is returned for any range after it started`() {
    val prn = prisonNumber()
    givenOpenCsip(prn, logCode = "OPEN", referral = "2024-01-10")

    assertThat(logCodes(prn, "2026-01-01", "2026-01-31")).containsExactly("OPEN")
    assertThat(logCodes(prn, "2023-01-01", "2023-12-31")).isEmpty()
  }

  @Test
  fun `a referral that ended at screening ends on the screening date`() {
    val prn = prisonNumber()
    val record = generateCsipRecord(person(prn), logCode = "NFA")
    dataSetup(record) {
      it.withReferral(incidentDate = LocalDate.parse("2024-01-08"), referralDate = LocalDate.parse("2024-01-10"))
      requireNotNull(it.referral).withSaferCustodyScreeningOutcome(
        outcome = givenReferenceData(SCREENING_OUTCOME_TYPE, "NFA"),
        date = LocalDate.parse("2024-01-12"),
      )
    }

    assertThat(logCodes(prn, "2024-01-12", "2024-12-31")).containsExactly("NFA")
    assertThat(logCodes(prn, "2024-01-13", "2024-12-31")).isEmpty()
  }

  private fun givenOpenCsip(prn: String, logCode: String, referral: String) {
    val record = generateCsipRecord(person(prn), logCode = logCode)
    dataSetup(record) {
      val date = LocalDate.parse(referral)
      it.withReferral(incidentDate = date.minusDays(2), referralDate = date)
      it.withPlan(firstCaseReviewDate = date.plusWeeks(4))
    }
  }

  private fun givenClosedCsip(prn: String, logCode: String, referral: String, closed: String) {
    val record = generateCsipRecord(person(prn), logCode = logCode)
    dataSetup(record) {
      val date = LocalDate.parse(referral)
      it.withReferral(incidentDate = date.minusDays(2), referralDate = date)
      it.withPlan(firstCaseReviewDate = date.plusWeeks(4))
      requireNotNull(it.plan).withReview(
        reviewDate = LocalDate.parse(closed),
        nextReviewDate = null,
        csipClosedDate = LocalDate.parse(closed),
        actions = sortedSetOf(ReviewAction.CLOSE_CSIP),
      )
    }
  }

  /** The prisoner's existing summary if an earlier CSIP created it, as a prison number has only one. */
  private fun person(prn: String) = personSummaryRepository.findByIdOrNull(prn) ?: prisoner(prisonerNumber = prn).toPersonSummary()

  private fun logCodes(prn: String, from: String? = null, to: String? = null): List<String> {
    val query = listOfNotNull("prn=$prn", from?.let { "fromDate=$it" }, to?.let { "toDate=$it" }).joinToString("&")
    val result = webTestClient.get().uri("/subject-access-request?$query")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .returnResult(JsonNode::class.java)
    if (result.status.value() == 204) {
      return emptyList()
    }
    assertThat(result.status.value()).isEqualTo(200)
    val body = result.responseBody.blockFirst()!!
    return body["content"].toList().map { it["logCode"].asString() }
  }
}
