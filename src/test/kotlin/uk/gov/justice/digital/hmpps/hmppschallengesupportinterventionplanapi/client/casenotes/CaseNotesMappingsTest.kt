package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class CaseNotesMappingsTest {

  @Test
  fun `should map case note to analysis item`() {
    val caseNote = testCaseNote()

    val result = caseNote.toCaseNoteAnalysisItem()

    assertThat(result.itemId)
      .isEqualTo(caseNote.caseNoteId.toString())

    assertThat(result.caseNoteText)
      .isEqualTo(caseNote.text)
  }

  @Test
  fun `should create jda request`() {
    val caseNote = testCaseNote()

    val response = testCaseNotesResponse(caseNote)

    val result = response.toJdaRequest("referral-id")

    assertThat(result.correlationId)
      .isEqualTo("referral-id")

    assertThat(result.prompt.key)
      .isEqualTo("case-note-analysis")

    assertThat(result.prompt.version)
      .isEqualTo(3)

    val requestData = result.requestData

    assertThat(requestData)
      .hasSize(1)

    assertThat(requestData.first().itemId)
      .isEqualTo(caseNote.caseNoteId.toString())

    assertThat(requestData.first().caseNoteText)
      .isEqualTo(caseNote.text)
  }

  @Test
  fun `should serialize jda request using documented contract`() {
    val response = testCaseNotesResponse()

    val result = response.toJdaRequest("referral-id")

    val json = ObjectMapper()
      .registerKotlinModule()
      .writeValueAsString(result)

    assertThat(json)
      .contains("\"correlationId\":\"referral-id\"")

    assertThat(json)
      .contains("\"key\":\"case-note-analysis\"")

    assertThat(json)
      .contains("\"version\":3")

    assertThat(json)
      .contains("\"requestData\":[")

    assertThat(json)
      .contains("\"item_id\"")

    assertThat(json)
      .contains("\"case_note_text\"")

    assertThat(json)
      .doesNotContain("\"caseNotes\"")
  }

  private fun testCaseNotesResponse(
    caseNote: CaseNote = testCaseNote(),
  ) = CaseNotesResponse(
    content = listOf(caseNote),
    hasCaseNotes = true,
    metadata = CaseNotesMetadata(
      totalElements = 1,
      page = 0,
      size = 1,
    ),
  )

  private fun testCaseNote() = CaseNote(
    caseNoteId = UUID.randomUUID(),
    offenderIdentifier = "A1234AA",
    type = "GEN",
    typeDescription = "General",
    subType = "OBS",
    subTypeDescription = "Observation",
    creationDateTime = LocalDateTime.now(),
    occurrenceDateTime = LocalDateTime.now(),
    authorName = "Test User",
    authorUserId = "USER1",
    authorUsername = "testuser",
    text = "Prisoner became agitated",
    locationId = "MDI",
    sensitive = false,
    amendments = emptyList(),
  )
}
