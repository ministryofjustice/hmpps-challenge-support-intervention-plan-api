package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.verifyAgainst
import java.time.Duration.ofSeconds

class PersonReconciliationIntTest : IntegrationTestBase() {

  @Test
  fun `reconciliation identifies differences and updates`() {
    val csip = dataSetup(generateCsipRecord()) { it.withCompletedReferral() }
    val prisoner = PrisonerDetails(csip.prisonNumber, "John", "Smith", "LEI", "IN", false, "LEI-A-1", null)
    givenPrisoners(listOf(prisoner))

    triggerReconciliation()

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    val ps = personSummaryRepository.findById(csip.prisonNumber).orElseThrow()
    ps.verifyAgainst(prisoner)
  }

  private fun triggerReconciliation() = webTestClient.post()
    .uri("/reconciliation/person")
    .exchange()
    .expectStatus().isOk
}
