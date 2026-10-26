package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Guards the data dictionary published to GitHub Pages (see migration/common/V60__schema_comments.sql
 * and V65__schema_comments_sensitivity.sql) and the SAR Data Requirements extract generated from the
 * same comments (V69__sar_data_requirements.sql).
 *
 * Descriptions live in the database as COMMENT ON statements so SchemaSpy, the CSV export and any
 * Glue crawl share one source of truth. Nothing else would notice a new column arriving undocumented,
 * and in this schema an undocumented column is quite likely to be health data about someone at risk
 * of self harm.
 *
 * Note the relkind filter includes 'v' as well as 'r'. csip_summary is a view, and the equivalent
 * test in the sibling services checks ordinary tables only - which would silently ignore all 17 of
 * its columns.
 */
class SchemaCommentsTest : IntegrationTestBase() {

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Test
  fun `every table and view has a description`() {
    val undocumented = jdbcTemplate.queryForList(
      """
      SELECT c.relname
      FROM pg_class c
      JOIN pg_namespace n ON n.oid = c.relnamespace
      WHERE n.nspname = 'public'
        AND c.relkind IN ('r', 'v')
        AND c.relname <> 'flyway_schema_history'
        AND obj_description(c.oid) IS NULL
      ORDER BY c.relname
      """.trimIndent(),
      String::class.java,
    )

    assertThat(undocumented)
      .describedAs("tables or views with no COMMENT ON - add one in a new migration")
      .isEmpty()
  }

  @Test
  fun `every column has a description`() {
    assertThat(columnComments().filter { it.comment == null }.map { it.name })
      .describedAs("columns with no COMMENT ON - add one in a new migration")
      .isEmpty()
  }

  @Test
  fun `every column description carries a sensitivity classification`() {
    val misclassified = columnComments()
      .filter { it.comment != null && !SENSITIVITY.containsMatchIn(it.comment) }
      .map { it.name }

    assertThat(misclassified)
      .describedAs("column comments must end with one of $SENSITIVITY - see V65__schema_comments_sensitivity.sql")
      .isEmpty()
  }

  /**
   * The answer matters more than the tag. A new column defaults to nothing, so whoever adds one has to
   * decide whether its value reaches a prisoner's subject access request report - and in this schema the
   * answer is usually yes, with the Offender SAR team to be told before it is released.
   */
  @Test
  fun `every column description says whether it is disclosed in a subject access request`() {
    val unclassified = columnComments()
      .filter { it.comment != null && !SAR_IMPACT.containsMatchIn(it.comment) }
      .map { it.name }

    assertThat(unclassified)
      .describedAs("column comments need a [SAR: Y] or [SAR: N] tag - see V69__sar_data_requirements.sql")
      .isEmpty()
  }

  @Test
  fun `every column description carries an example value`() {
    val missing = columnComments()
      .filter { it.comment != null && !EXAMPLE.containsMatchIn(it.comment) }
      .map { it.name }

    assertThat(missing)
      .describedAs("column comments need an [Example: ...] tag - see V69__sar_data_requirements.sql")
      .isEmpty()
  }

  private data class ColumnComment(
    val name: String,
    val comment: String?,
  )

  private fun columnComments(): List<ColumnComment> = jdbcTemplate.query(
    """
    SELECT c.relname || '.' || a.attname          AS name,
           col_description(c.oid, a.attnum)       AS comment
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped
    WHERE n.nspname = 'public'
      AND c.relkind IN ('r', 'v')
      AND c.relname <> 'flyway_schema_history'
    ORDER BY c.relname, a.attnum
    """.trimIndent(),
  ) { rs, _ -> ColumnComment(rs.getString("name"), rs.getString("comment")) }

  private companion object {
    val SENSITIVITY = Regex("""\[Sensitivity: (NONE|PERSONAL|STAFF|SPECIAL-CATEGORY|OFFICIAL-SENSITIVE)]$""")

    // Both sit before the sensitivity tag, which SENSITIVITY anchors to the end of the comment. The SAR
    // Data Requirements extract the Offender SAR team review needs both for every element.
    val EXAMPLE = Regex("""\[Example: [^]]+]""")
    val SAR_IMPACT = Regex("""\[SAR: [YN]]""")
  }
}
