package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
  description = "The role of the person signing off the decision to a CSIP referral"
)
data class DecisionSignerRole(
  @Schema(
    description = "The short code to refer to the Decision Signer Role",
  )
  val code: String,

  @Schema(
    description = "The description of the Decision Signer Role",
  )
  val description: String?,

  @Schema(
    description = "The sequence number of the Decision Signer Role. " +
      "Used for ordering Types correctly in lists and drop downs. ",
    example = "3",
  )
  val listSequence: Int?,

  @Schema(
    description = "The date and time the Decision Signer Role was created",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdAt: LocalDateTime,

  @Schema(
    description = "The username of the user who created the Decision Signer Role",
    example = "USER1234",
  )
  val createdBy: String,

  @Schema(
    description = "The date and time the Decision Signer Role was last modified",
    example = "2022-07-15T15:24:56",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val modifiedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who last modified the Decision Signer Role",
    example = "USER1234",
  )
  val modifiedBy: String?,

  @Schema(
    description = "The date and time the Decision Signer Role was deactivated",
    example = "2023-11-08T09:53:34",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val deactivatedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who deactivated the Decision Signer Role",
    example = "USER1234",
  )
  val deactivatedBy: String?,
)
