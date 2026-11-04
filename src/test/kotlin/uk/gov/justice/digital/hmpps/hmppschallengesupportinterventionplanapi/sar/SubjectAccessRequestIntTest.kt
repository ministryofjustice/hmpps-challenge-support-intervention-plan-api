package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sar

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.repository.findByIdOrNull
import tools.jackson.databind.JsonNode
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.SCREENING_OUTCOME_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.NomisIdGenerator.prisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.set
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
  fun `an open CSIP runs from when it started to today`() {
    val prn = prisonNumber()
    val today = LocalDate.now()
    givenOpenCsip(prn, logCode = "OPEN", referral = "2024-01-10")

    assertThat(logCodes(prn, "2024-06-01", "2024-06-30")).containsExactly("OPEN")
    assertThat(logCodes(prn, today.toString(), today.plusDays(30).toString())).containsExactly("OPEN")
    assertThat(logCodes(prn, today.plusDays(1).toString(), today.plusDays(30).toString())).isEmpty()
    assertThat(logCodes(prn, "2023-01-01", "2023-12-31")).isEmpty()
  }

  @Test
  fun `an open CSIP with a later recorded date runs to that date`() {
    val prn = prisonNumber()
    val tomorrow = LocalDate.now().plusDays(1)
    givenOpenCsip(prn, logCode = "OPEN", referral = tomorrow.toString())

    assertThat(logCodes(prn, tomorrow.toString(), tomorrow.toString())).containsExactly("OPEN")
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

  @Test
  fun `a staff username synced from NOMIS as the display name is not sent`() {
    val prn = prisonNumber()
    val record = generateCsipRecord(person(prn), logCode = "SYNCED")
    dataSetup(record) {
      it.withReferral()
      val referral = requireNotNull(it.referral)
      referral.withSaferCustodyScreeningOutcome(
        outcome = givenReferenceData(SCREENING_OUTCOME_TYPE, "NFA"),
        recordedBy = "JSMITH_GEN",
        recordedByDisplayName = "JSMITH_GEN",
      )
      referral.set(referral::investigation, Investigation(referral, "JSMITH_GEN", "JSMITH_GEN"))
    }

    val csip = content(prn).single()

    assertThat(csip["referral"].has("saferCustodyScreeningOutcome")).isTrue()
    assertThat(csip["referral"].has("investigation")).isTrue()
    assertThat(csip.toString()).doesNotContainIgnoringCase("JSMITH_GEN")
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

  private fun logCodes(prn: String, from: String? = null, to: String? = null): List<String> = content(prn, from, to).map { it["logCode"].asString() }

  private fun content(prn: String, from: String? = null, to: String? = null): List<JsonNode> {
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
    return body["content"].toList()
  }
}
