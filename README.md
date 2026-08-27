# hmpps-challenge-support-intervention-plan-api
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-challenge-support-intervention-plan-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-challenge-support-intervention-plan-api "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-challenge-support-intervention-plan-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-challenge-support-intervention-plan-api)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-challenge-support-intervention-plan-api/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-challenge-support-intervention-plan-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://csip-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)
[![Event docs](https://img.shields.io/badge/Event_docs-view-85EA2D.svg)](https://studio.asyncapi.com/?readOnly&url=https://raw.githubusercontent.com/ministryofjustice/hmpps-challenge-support-intervention-plan-api/main/async-api.yml)

Datebase Schema diagram: https://ministryofjustice.github.io/hmpps-challenge-support-intervention-plan-api/schema-spy-report/

## HMPPS Project Setup instructions

For more instructions and general hmpps project setup guidelines:
- [Running the service locally using run-local.sh](docs/RUNNING_LOCALLY.md).
- Command line for retrieving secrets in k8s dev namespace:
  - ```kubectl -n <dev-namespace-here> get secret <secret-name-here> -o json | jq -r ".data | map_values(@base64d)"```
- Tool to check kubernetes instances in k8s dev namesapce:
  - ```k9s -n <dev-namespace-here>```
## Data dictionary

A browsable schema report is published from `main` to
[ministryofjustice.github.io/hmpps-challenge-support-intervention-plan-api/schema-spy-report](https://ministryofjustice.github.io/hmpps-challenge-support-intervention-plan-api/schema-spy-report/),
along with two CSV exports for the MOJ Data Catalogue and SAR assurance:

| File | Contents |
|------|----------|
| `data-dictionary.csv` | Every table, view and column, with its description, sensitivity classification, type, nullability, PK and FK |
| `reference-data.csv` | The `reference_data` rows, plus the enums persisted as strings with no table behind them - `decision_and_actions.actions`, `review.actions`, `audit_revision.source` and the rest resolve in Kotlin only |

Table and column descriptions live in `src/main/resources/migration/common/` as `COMMENT ON`
statements (`V60__schema_comments.sql` and `V65__schema_comments_sensitivity.sql`), so the database is
the single source of truth and SchemaSpy, the CSV export and any Glue crawl all agree. **Add a
`COMMENT ON` for any new table, view or column** - a later migration can add to or replace comments at
any time.

### Data sensitivity

Every column comment ends with a sensitivity classification:

| Tag | Meaning |
|-----|---------|
| `NONE` | Not personal data in itself - keys, timestamps, process flags |
| `PERSONAL` | Personal data about a prisoner - identifies or locates them |
| `STAFF` | Personal data about a member of staff, typically the username that acted |
| `SPECIAL-CATEGORY` | UK GDPR Article 9 data, or offence data under Article 10 |
| `OFFICIAL-SENSITIVE` | Not personal data, but damaging if disclosed |

Two rules decide the awkward cases. The tag describes **the column's own content, not the row's** -
every row here concerns someone believed to be at risk, so the record is sensitive whatever a single
column is marked. And **every free-text column is assumed to contain more than its label asks**.

This is the most Article 9 heavy schema in Manage Safety: CSIP exists to manage risk of self-harm and
suicide, so triggers, protective factors, usual behaviour and the screening decision are health data
about the person, not merely notes about a process. **The audit tables carry the same tag as the
column they mirror** - Envers records the full history of every mutable property, so an audit row
holds the same content as the live row and a subject access request has to reach it.

`SchemaCommentsTest` runs in the normal build and fails if a table, view or column has no comment, or
if a column comment does not end in a valid tag. Note it checks views as well as tables, so
`csip_summary` is covered.

To regenerate locally:

```bash
docker compose -f docker-compose-schema-spy.yml up -d --wait
./gradlew -Pinit-db=true test --tests '*InitialiseDatabase' --tests '*ExportReferenceData'
docker run --rm --network host -v /tmp/schemaspy:/output schemaspy/schemaspy:6.2.4 \
  -t pgsql -host localhost -port 5432 -db csip -s public -u csip -p csip -vizjs
scripts/generate-data-dictionary.sh
```

Two things to know when reading the generated report. The test profile adds
`classpath:/migration/test`, so a locally built database also contains the test-only reference data
migrations. And if you edit a migration while the compose Postgres is still running, tear it down with
`docker compose -f docker-compose-schema-spy.yml down -v` first, or Flyway fails on a checksum
mismatch.
