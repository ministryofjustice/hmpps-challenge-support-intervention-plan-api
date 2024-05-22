package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
  description = "A type to categorise the contributory factor to the incident or motivation for CSIP referral.",
)
data class ContributoryFactorType(
  @Schema(
    description = "The short code to refer to the Contributory Factor Type",
  )
  val code: String,

  @Schema(
    description = "The description of the Contributory Factor Type",
  )
  val description: String?,

  @Schema(
    description = "The sequence number of the Contributory Factor Type. " +
      "Used for ordering Types correctly in lists and drop downs. ",
    example = "3",
  )
  val listSequence: Int?,

  @Schema(
    description = "The date and time the Contributory Factor Type was created",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdAt: LocalDateTime,

  @Schema(
    description = "The username of the user who created the Contributory Factor Type",
    example = "USER1234",
  )
  val createdBy: String,

  @Schema(
    description = "The date and time the Contributory Factor Type was last modified",
    example = "2022-07-15T15:24:56",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val modifiedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who last modified the Contributory Factor Type",
    example = "USER1234",
  )
  val modifiedBy: String?,

  @Schema(
    description = "The date and time the Contributory Factor Type was deactivated",
    example = "2023-11-08T09:53:34",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val deactivatedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who deactivated the Contributory Factor Type",
    example = "USER1234",
  )
  val deactivatedBy: String?,
)
