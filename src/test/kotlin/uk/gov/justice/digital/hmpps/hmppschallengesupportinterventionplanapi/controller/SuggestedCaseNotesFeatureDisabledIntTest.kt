package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_PRISONER_CASE_NOTES_RO
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.SuggestedCaseNotesRequest

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = ["feature.suggested-case-notes=false"])
class SuggestedCaseNotesFeatureDisabledIntTest : IntegrationTestBase() {

  @Test
  fun `feature disabled keeps existing error handling behaviour`() {
    val response = webTestClient.post()
      .uri("/v1/suggestedCaseNotes/A1234AA")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_CASE_NOTES_RO)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        SuggestedCaseNotesRequest(
          referralId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
          sortField = "creationDateTime",
          sortOrder = "desc",
        ),
      )
      .exchange()
      .errorResponse(HttpStatus.INTERNAL_SERVER_ERROR)

    assertThat(response.developerMessage).contains("405 METHOD_NOT_ALLOWED")
    assertThat(response.developerMessage).contains("suggestedCaseNotes feature is not enabled")
  }
}
