package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
  description = "The Location of the incident that motivated the CSIP Referral",
)
data class IncidentLocation(
  @Schema(
    description = "The short code to refer to the Incident Location",
  )
  val code: String,

  @Schema(
    description = "The description of the Incident Location",
  )
  val description: String?,

  @Schema(
    description = "The sequence number of the Incident Location. " +
      "Used for ordering Types correctly in lists and drop downs. ",
    example = "3",
  )
  val listSequence: Int?,

  @Schema(
    description = "The date and time the Incident Location was created",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdAt: LocalDateTime,

  @Schema(
    description = "The username of the user who created the Incident Location",
    example = "USER1234",
  )
  val createdBy: String,

  @Schema(
    description = "The date and time the Incident Location was last modified",
    example = "2022-07-15T15:24:56",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val modifiedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who last modified the Incident Location",
    example = "USER1234",
  )
  val modifiedBy: String?,

  @Schema(
    description = "The date and time the Incident Location was deactivated",
    example = "2023-11-08T09:53:34",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val deactivatedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who deactivated the Incident Location",
    example = "USER1234",
  )
  val deactivatedBy: String?,
)