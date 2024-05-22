package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
  description = "The role of the interviewee in the investigation on a CSIP referral",
)
data class IntervieweeRole(
  @Schema(
    description = "The short code to refer to the Interviewee Role",
  )
  val code: String,

  @Schema(
    description = "The description of the Interviewee Role",
  )
  val description: String?,

  @Schema(
    description = "The sequence number of the Interviewee Role. " +
      "Used for ordering Types correctly in lists and drop downs. ",
    example = "3",
  )
  val listSequence: Int?,

  @Schema(
    description = "The date and time the Interviewee Role was created",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdAt: LocalDateTime,

  @Schema(
    description = "The username of the user who created the Interviewee Role",
    example = "USER1234",
  )
  val createdBy: String,

  @Schema(
    description = "The date and time the Interviewee Role was last modified",
    example = "2022-07-15T15:24:56",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val modifiedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who last modified the Interviewee Role",
    example = "USER1234",
  )
  val modifiedBy: String?,

  @Schema(
    description = "The date and time the Interviewee Role was deactivated",
    example = "2023-11-08T09:53:34",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val deactivatedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who deactivated the Interviewee Role",
    example = "USER1234",
  )
  val deactivatedBy: String?,
)
