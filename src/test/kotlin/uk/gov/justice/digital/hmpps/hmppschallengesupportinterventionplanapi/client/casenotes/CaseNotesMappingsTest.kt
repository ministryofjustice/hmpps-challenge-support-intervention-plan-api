package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jsonMapper
import java.time.LocalDateTime
import java.util.UUID

class CaseNotesMappingsTest {

  private val jsonMapper = jsonMapper()

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
    val caseNote = testCaseNote()

    val response = testCaseNotesResponse(caseNote)

    val result = response.toJdaRequest("referral-id")

    val jsonNode = jsonMapper.readTree(
      jsonMapper.writeValueAsString(result),
    )

    assertThat(jsonNode["correlationId"].asText())
      .isEqualTo("referral-id")

    assertThat(jsonNode["prompt"]["key"].asText())
      .isEqualTo("case-note-analysis")

    assertThat(jsonNode["prompt"]["version"].asInt())
      .isEqualTo(3)

    assertThat(jsonNode["requestData"].isArray)
      .isTrue()

    assertThat(jsonNode["requestData"].size())
      .isEqualTo(1)

    assertThat(jsonNode["requestData"][0]["item_id"].asText())
      .isEqualTo(caseNote.caseNoteId.toString())

    assertThat(jsonNode["requestData"][0]["case_note_text"].asText())
      .isEqualTo(caseNote.text)

    assertThat(jsonNode["requestData"][0].has("caseNotes"))
      .isFalse()
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
