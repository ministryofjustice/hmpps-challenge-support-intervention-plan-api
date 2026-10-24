#!/usr/bin/env bash
#
# Exports the SAR Data Requirements extract that the Offender SAR team review.
#
# This is the document behind the data review checkpoint of the Central SAR Change Control Process
# (IR-2067). It is generated rather than hand-written for the same reason the data dictionary is: the
# schema comments are the one source of truth, so the extract cannot drift from the schema.
#
# Columns are those the Offender SAR team ask for:
#
#   Product Name, Type of Change, Entity, Element, Description, Example Value, Mandatory, SAR Impact, Impact
#
# Example Value and SAR Impact come from the [Example: ...] and [SAR: Y|N] tags on each column comment, set
# per column in V69__sar_data_requirements.sql. SAR Impact answers the question the Offender SAR team are
# actually asking: does this element's value reach the prisoner's report. It is deliberately not derived from
# the [Sensitivity: ...] tag, which answers a different question - whether the column's own content is
# personal data.
#
# Type of Change is NEW PRODUCT and Impact is NO CHANGE throughout, which is right for a product going through
# the process for the first time. Both can be overridden for a later Enhancement or Maintenance review, where
# Impact will need setting per element on the copy that is sent.
#
# Usage:
#   scripts/generate-sar-data-requirements.sh [output-file]
#
# Expects a database built by Flyway. Connection details are taken from the environment, defaulting
# to the container in docker-compose-schema-spy.yml:
#   DB_HOST (localhost) DB_PORT (5432) DB_NAME (csip) DB_USER (csip)
#   DB_PASSWORD (csip) DB_SCHEMA (public)
#
# PRODUCT_NAME, CHANGE_TYPE and IMPACT can be overridden the same way.

set -euo pipefail

OUTPUT="${1:-sar-data-requirements.csv}"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-csip}"
DB_USER="${DB_USER:-csip}"
DB_PASSWORD="${DB_PASSWORD:-csip}"
DB_SCHEMA="${DB_SCHEMA:-public}"

PRODUCT_NAME="${PRODUCT_NAME:-Challenge, Support and Intervention Plan (CSIP)}"
CHANGE_TYPE="${CHANGE_TYPE:-NEW PRODUCT}"
IMPACT="${IMPACT:-NO CHANGE}"

# The values above go into the query as SQL string literals, so quote them properly: an override containing
# an apostrophe would otherwise end the literal early and break the query.
sql_literal() {
  local quote="'"
  printf "'%s'" "${1//$quote/$quote$quote}"
}

PRODUCT_NAME_SQL="$(sql_literal "$PRODUCT_NAME")"
CHANGE_TYPE_SQL="$(sql_literal "$CHANGE_TYPE")"
IMPACT_SQL="$(sql_literal "$IMPACT")"
DB_SCHEMA_SQL="$(sql_literal "$DB_SCHEMA")"

export PGPASSWORD="$DB_PASSWORD"

# relkind 'v' is included alongside 'r' so csip_summary appears, as in the data dictionary. Its columns
# repeat columns of the tables it joins, but the Offender SAR team should see every element the service holds.
read -r -d '' QUERY <<SQL || true
SELECT
  ${PRODUCT_NAME_SQL}                                    AS "Product Name",
  ${CHANGE_TYPE_SQL}                                     AS "Type of Change",
  c.relname                                              AS "Entity",
  a.attname                                              AS "Element",
  regexp_replace(
    col_description(c.oid, a.attnum),
    '\s*\[(Example|SAR|Sensitivity): [^\]]*\]', '', 'g'
  )                                                      AS "Description",
  substring(
    col_description(c.oid, a.attnum)
    from '\[Example: ([^\]]*)\]'
  )                                                      AS "Example Value",
  CASE WHEN a.attnotnull THEN 'Y' ELSE 'N' END           AS "Mandatory",
  substring(
    col_description(c.oid, a.attnum)
    from '\[SAR: ([YN])\]'
  )                                                      AS "SAR Impact",
  ${IMPACT_SQL}                                          AS "Impact"
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped
WHERE n.nspname = ${DB_SCHEMA_SQL}
  AND c.relkind IN ('r', 'v')
  AND c.relname <> 'flyway_schema_history'
ORDER BY c.relname, a.attnum
SQL

COPY_COMMAND="COPY ($QUERY) TO STDOUT WITH (FORMAT csv, HEADER true)"

if command -v psql > /dev/null 2>&1; then
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
    -v ON_ERROR_STOP=1 -c "$COPY_COMMAND" > "$OUTPUT"
else
  # No local client - use the postgres image. host.docker.internal resolves on Docker Desktop,
  # and --add-host makes it resolve on Linux too.
  docker run --rm --add-host=host.docker.internal:host-gateway \
    -e PGPASSWORD="$DB_PASSWORD" postgres:18 \
    psql -h "$([ "$DB_HOST" = "localhost" ] && echo host.docker.internal || echo "$DB_HOST")" \
    -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
    -v ON_ERROR_STOP=1 -c "$COPY_COMMAND" > "$OUTPUT"
fi

echo "Wrote $(($(wc -l < "$OUTPUT") - 1)) elements to $OUTPUT"
