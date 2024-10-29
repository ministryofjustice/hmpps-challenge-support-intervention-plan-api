package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referencedata.ReferenceData
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "The contributory factor to the incident that motivated the CSIP referral")
data class ContributoryFactor(
  @Schema(
    description = "The unique identifier assigned to the Contributory Factor",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val factorUuid: UUID,

  @Schema(description = "The type of contributory factor to the incident or motivation for CSIP referral.")
  val factorType: ReferenceData,

  @Schema(description = "Additional information about the contributory factor to the incident or motivation for CSIP referral.")
  val comment: String?,

  @Schema(description = "The date and time the Contributory Factor was created", example = "2021-09-27T14:19:25")
  val createdAt: LocalDateTime,

  @Schema(description = "The username of the user who created the Contributory Factor", example = "USER1234")
  val createdBy: String,

  @Schema(description = "The date and time the Contributory Factor was last modified", example = "2022-07-15T15:24:56")
  val lastModifiedAt: LocalDateTime?,

  @Schema(description = "The username of the user who last modified the Contributory Factor", example = "USER1234")
  val lastModifiedBy: String?,
)
