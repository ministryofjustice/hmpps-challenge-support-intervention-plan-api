package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.io.File

/**
 * Writes reference-data.csv, published alongside the SchemaSpy report and the CSV data dictionary.
 *
 * Two kinds of code list end up in this schema and neither is discoverable from the schema report
 * alone:
 *
 *  - The rows in [reference_data], a single consolidated table keyed on (domain, code). V4 collapsed
 *    eight separate code tables into it, so the schema report shows one table where a reader expects
 *    eight lookups.
 *  - Enums that are persisted as strings with no table behind them at all. A reader of the schema sees
 *    a varchar and has no way to learn the permitted values.
 *
 * Not part of the normal suite - see the -Pinit-db toggle in build.gradle.kts.
 */
class ExportReferenceData : IntegrationTestBase() {

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Test
  fun `exports reference data`() {
    val rows = enumRows() + referenceDataRows()

    assertThat(rows.filter { it.description.isBlank() }.map { "${it.columnRef}.${it.code}" })
      .describedAs("every exported value needs a description - an undescribed code is not reference data")
      .isEmpty()

    val output = File(System.getProperty("referenceDataOutput") ?: "reference-data.csv")
    output.printWriter().use { out ->
      out.println("column_ref,code,description,notes")
      rows.forEach { out.println(it.toCsv()) }
    }
    println("Wrote ${rows.size} reference data values to ${output.absolutePath}")
  }

  /**
   * The live contents of the reference_data table, which is data rather than code and so can change
   * without a release.
   */
  private fun referenceDataRows(): List<Row> = jdbcTemplate.query(
    """
    SELECT domain, code, description, deactivated_at IS NOT NULL AS deactivated
    FROM reference_data
    ORDER BY domain, list_sequence, code
    """.trimIndent(),
  ) { rs, _ ->
    Row(
      columnRef = "reference_data.${rs.getString("domain").lowercase()}",
      code = rs.getString("code"),
      description = rs.getString("description"),
      notes = if (rs.getBoolean("deactivated")) "deactivated - retained for historic records" else "",
    )
  }

  private fun enumRows(): List<Row> = buildList {
    ReferenceDataType.entries.forEach {
      add(
        Row(
          "reference_data.domain",
          it.name,
          "Reference data domain. Exposed by the API as '${it.domain}'.",
          "the domain half of the (domain, code) key",
        ),
      )
    }
    DecisionAction.entries.forEach { add(Row("decision_and_actions.actions", it.name, DECISION_ACTIONS[it]!!, NOMIS_ONLY)) }
    ReviewAction.entries.forEach {
      add(Row("review.actions", it.name, REVIEW_ACTIONS[it]!!, if (it == ReviewAction.CLOSE_CSIP) "the only value set by DPS" else NOMIS_ONLY))
    }
    OptionalYesNoAnswer.entries.forEach { add(Row("referral.safer_custody_team_informed", it.name, YES_NO[it]!!, "")) }
    Source.entries.forEach { add(Row("audit_revision.source", it.name, SOURCES[it]!!, "")) }
    CsipComponent.entries.forEach {
      add(Row("audit_revision.affected_components", it.name, "The ${it.name.lowercase().replace('_', ' ')} was affected by the change.", ""))
    }
    BehaviourType.entries.forEach { add(Row("case_note_annotations.behaviour_type", it.name, BEHAVIOUR_TYPES[it]!!, AI_NOTE)) }
    ConfidenceLevel.entries.forEach {
      add(Row("case_note_annotations.confidence_level", it.name, "${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }} confidence in the classification of the source case note.", AI_NOTE))
    }
  }

  private data class Row(val columnRef: String, val code: String, val description: String, val notes: String) {
    fun toCsv() = listOf(columnRef, code, description, notes).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
  }

  private companion object {
    const val NOMIS_ONLY = "populated via NOMIS only, retained for sync and reporting"
    const val AI_NOTE = "machine generated, not recorded by staff"

    val DECISION_ACTIONS = mapOf(
      DecisionAction.OPEN_CSIP_ALERT to "A CSIP alert should be opened against the person.",
      DecisionAction.NON_ASSOCIATIONS_UPDATED to "The person's non-associations were updated as a result.",
      DecisionAction.OBSERVATION_BOOK to "The person should be recorded in the observation book.",
      DecisionAction.UNIT_OR_CELL_MOVE to "The person should be moved to a different unit or cell.",
      DecisionAction.CSRA_OR_RSRA_REVIEW to "A cell sharing or room sharing risk assessment should be reviewed.",
      DecisionAction.SERVICE_REFERRAL to "The person should be referred to another service.",
      DecisionAction.SIM_REFERRAL to "The person should be referred to the Safety Intervention Meeting.",
    )
    val REVIEW_ACTIONS = mapOf(
      ReviewAction.RESPONSIBLE_PEOPLE_INFORMED to "The people responsible for the person's care were informed.",
      ReviewAction.CSIP_UPDATED to "The CSIP was updated following the review.",
      ReviewAction.REMAIN_ON_CSIP to "The person should remain on a CSIP.",
      ReviewAction.CASE_NOTE to "A case note was recorded following the review.",
      ReviewAction.CLOSE_CSIP to "The CSIP should be closed. Sets plan.closed_date.",
    )
    val YES_NO = mapOf(
      OptionalYesNoAnswer.YES to "Yes.",
      OptionalYesNoAnswer.NO to "No.",
      OptionalYesNoAnswer.DO_NOT_KNOW to "Not known. Distinguishes an unanswered question from an answer of no.",
    )
    val SOURCES = mapOf(
      Source.DPS to "The change was made in DPS.",
      Source.NOMIS to "The change was made in NOMIS and synchronised into this service.",
    )
    val BEHAVIOUR_TYPES = mapOf(
      BehaviourType.RISKS_AND_TRIGGERS to "The extract was inferred to evidence risks and triggers for the person.",
      BehaviourType.USUAL_BEHAVIOUR_PRESENTATION to "The extract was inferred to evidence the person's usual behaviour and presentation.",
      BehaviourType.PROTECTIVE_FACTORS to "The extract was inferred to evidence protective factors for the person.",
    )
  }
}
