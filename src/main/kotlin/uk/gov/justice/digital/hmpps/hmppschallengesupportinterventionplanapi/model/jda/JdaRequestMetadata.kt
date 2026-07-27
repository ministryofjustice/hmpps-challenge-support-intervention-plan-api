package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import java.time.OffsetDateTime

data class JdaRequestMetadata(
  val requestType: JdaRequestType,
  val submittedAt: OffsetDateTime,
)
