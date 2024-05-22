package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
  description = "A reference data code to categorise various aspects related to a CSIP record",
)
data class ReferenceData(
  @Schema(
    description = "The short code of a reference data",
  )
  val code: String,

  @Schema(
    description = "The description of the reference data code",
  )
  val description: String?,

  @Schema(
    description = "The sequence number of the code. " +
      "Used for ordering codes correctly in lists and drop downs. ",
    example = "3",
  )
  val listSequence: Int?,

  @Schema(
    description = "The date and time the code was created",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdAt: LocalDateTime,

  @Schema(
    description = "The username of the user who created the code",
    example = "USER1234",
  )
  val createdBy: String,

  @Schema(
    description = "The date and time the code was last modified",
    example = "2022-07-15T15:24:56",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val modifiedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who last modified the code",
    example = "USER1234",
  )
  val modifiedBy: String?,

  @Schema(
    description = "The date and time the code was deactivated",
    example = "2023-11-08T09:53:34",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val deactivatedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who deactivated the code",
    example = "USER1234",
  )
  val deactivatedBy: String?,
)
