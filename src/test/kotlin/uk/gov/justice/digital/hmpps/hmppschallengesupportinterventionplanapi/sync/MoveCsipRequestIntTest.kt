package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_MOVED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.NomisIdGenerator.prisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
import java.time.Duration.ofSeconds
import java.util.UUID

class MoveCsipRequestIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.put().uri(urlToTest()).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - wrong role`() {
    val response = webTestClient.put().uri(urlToTest())
      .headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .bodyValue(moveCsipRequest())
      .exchange().errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `request is idempotent - success if csip records are already moved`() {
    moveCsipRecord(moveCsipRequest(ids = setOf(UUID.randomUUID(), UUID.randomUUID())))
  }

  @Test
  fun `csip records moved to new prison number`() {
    val oldNoms = prisonNumber()
    val newNoms = prisonNumber()
    val oldPerson = prisoner(oldNoms).toPersonSummary()

    val csip1 = dataSetup(generateCsipRecord(oldPerson)) { it.withCompletedReferral() }
    val csip2 = dataSetup(generateCsipRecord(oldPerson)) { it.withCompletedReferral() }

    dataSetup(generateCsipRecord(prisoner(newNoms).toPersonSummary())) { it.withCompletedReferral() }

    moveCsipRecord(moveCsipRequest(oldNoms, newNoms, setOf(csip1.id, csip2.id)))

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    assertThat(csipRecordRepository.countByPrisonNumber(newNoms)).isEqualTo(3)
    assertThat(personSummaryRepository.findByIdOrNull(oldNoms)).isNull()

    verifyDomainEvents(newNoms, setOf(csip1.id, csip2.id), CSIP_MOVED, 2, Source.NOMIS, oldNoms)
  }

  @Test
  fun `person record created and csip record moved to new prison number`() {
    val oldNoms = prisonNumber()
    val newNoms = prisonNumber()
    givenValidPrisonNumber(newNoms)
    assertThat(personSummaryRepository.findByIdOrNull(newNoms)).isNull()

    val oldPerson = prisoner(oldNoms).toPersonSummary()
    val csip1 = dataSetup(generateCsipRecord(oldPerson)) { it.withCompletedReferral() }
    dataSetup(generateCsipRecord(oldPerson)) { it.withCompletedReferral() }

    moveCsipRecord(moveCsipRequest(oldNoms, newNoms, setOf(csip1.id)))

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    assertThat(csipRecordRepository.countByPrisonNumber(newNoms)).isEqualTo(1)

    verifyDomainEvents(newNoms, setOf(csip1.id), CSIP_MOVED, 1, Source.NOMIS, oldNoms)
  }

  private fun urlToTest() = "/sync/csip-records/move"

  private fun moveCsipRequest(from: String = prisonNumber(), to: String = prisonNumber(), ids: Set<UUID> = setOf()) =
    MoveCsipRequest(from, to, ids)

  private fun syncCsipResponseSpec(
    request: MoveCsipRequest,
  ) = webTestClient.put().uri(urlToTest())
    .bodyValue(request)
    .headers(setAuthorisation(isUserToken = false, roles = listOf(ROLE_NOMIS))).exchange()

  private fun moveCsipRecord(request: MoveCsipRequest) = syncCsipResponseSpec(request).expectStatus().isOk
}
