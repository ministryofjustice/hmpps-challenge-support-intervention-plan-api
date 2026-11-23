ARG BASE_IMAGE=ghcr.io/ministryofjustice/hmpps-distroless-java:25-jre
FROM --platform=$BUILDPLATFORM ${BASE_IMAGE} AS builder

ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}

WORKDIR /builder
COPY hmpps-challenge-support-intervention-plan-api-${BUILD_NUMBER}.jar app.jar
RUN ["java", "-Djarmode=tools", "-jar", "app.jar", "extract", "--layers", "--destination", "extracted"]

FROM ${BASE_IMAGE}
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"

ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}

WORKDIR /app
# The distroless UID has no account entry. Keep the home directory used for the PostgreSQL CA bundle.
ENV HOME=/home/appuser

COPY --chown=2000:2000 applicationinsights.json ./
COPY --chown=2000:2000 applicationinsights.dev.json ./
COPY --chown=2000:2000 applicationinsights-agent*.jar ./agent.jar
COPY --from=builder --chown=2000:2000 /builder/extracted/dependencies/ ./
COPY --from=builder --chown=2000:2000 /builder/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=2000:2000 /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=2000:2000 /builder/extracted/application/ ./

ENTRYPOINT ["java", "-Duser.home=/home/appuser", "-XX:+ExitOnOutOfMemoryError", "-XX:+AlwaysActAsServerClassMachine", "-javaagent:agent.jar", "-jar", "app.jar"]
