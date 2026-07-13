package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.stubbing.StubMapping

internal const val OFFENDER_IDENTIFIER = "A1234AA"
internal const val OFFENDER_IDENTIFIER_NOT_FOUND = "NOT_FOUND"
internal const val OFFENDER_IDENTIFIER_THROW_EXCEPTION = "THROW"

class CaseNotesServer : WireMockServer(8113) {
  fun stubGetCaseNotes(offenderIdentifier: String = OFFENDER_IDENTIFIER): StubMapping = stubFor(
    post("/search/case-notes/$offenderIdentifier")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "content": [
                {
                  "caseNoteId": "123e4567-e89b-12d3-a456-426614174000",
                  "offenderIdentifier": "$offenderIdentifier",
                  "type": "TYPE",
                  "typeDescription": "Type description",
                  "subType": "SUB_TYPE",
                  "subTypeDescription": "Subtype description",
                  "creationDateTime": "2026-07-09T12:00:00",
                  "occurrenceDateTime": "2026-07-09T11:30:00",
                  "authorName": "Author Name",
                  "authorUserId": "USER123",
                  "authorUsername": "author.username",
                  "text": "Some case note text",
                  "locationId": "LEI",
                  "sensitive": false,
                  "amendments": [
                    {
                      "creationDateTime": "2026-07-09T12:05:00",
                      "authorUserName": "author.username",
                      "authorName": "Author Name",
                      "authorUserId": "USER123",
                      "additionalNoteText": "Amendment text",
                      "id": "223e4567-e89b-12d3-a456-426614174000"
                    }
                  ]
                }
              ],
              "hasCaseNotes": true
            }
            """.trimIndent(),
          )
          .withStatus(200),
      ),
  )

  fun stubGetCaseNotesException(offenderIdentifier: String = OFFENDER_IDENTIFIER_THROW_EXCEPTION): StubMapping = stubFor(
    post("/search/case-notes/$offenderIdentifier")
      .willReturn(aResponse().withStatus(500)),
  )
}
