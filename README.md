# hmpps-challenge-support-intervention-plan-api
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-challenge-support-intervention-plan-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-challenge-support-intervention-plan-api "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-challenge-support-intervention-plan-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-challenge-support-intervention-plan-api)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-challenge-support-intervention-plan-api/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-challenge-support-intervention-plan-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://hmpps-challenge-support-intervention-plan-api-dev.hmpps.service.justice.gov.uk/webjars/swagger-ui/index.html?configUrl=/v3/api-docs)
[![Event docs](https://img.shields.io/badge/Event_docs-view-85EA2D.svg)](https://studio.asyncapi.com/?readOnly&url=https://raw.githubusercontent.com/ministryofjustice/hmpps-challenge-support-intervention-plan-api/main/async-api.yml)

Datebase Schema diagram: https://ministryofjustice.github.io/hmpps-challenge-support-intervention-plan-api/schema-spy-report/

## HMPPS Project Setup instructions

For more instructions and general hmpps project setup guidelines:
- [Running the service locally using run-local.sh](docs/RUNNING_LOCALLY.md).
- Command line for retrieving secrets in k8s dev namespace:
  - ```kubectl -n <dev-namespace-here> get secret <secret-name-here> -o json | jq -r ".data | map_values(@base64d)"```
- Tool to check kubernetes instances in k8s dev namesapce:
  - ```k9s -n <dev-namespace-here>```