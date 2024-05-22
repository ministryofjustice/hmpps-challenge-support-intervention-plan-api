package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.IntervieweeRole
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(
  description = "An interview in relation to the investigation on the incident that motivated the CSIP referral"
)
data class Interview (
  @Schema(
    description = "The unique identifier assigned to the Interview",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val interviewUuid: UUID,

  @Schema(
    description = "Name of the person being interviewed.",
  )
  val interviewee: String,

  @Schema(
    description = "The date the interview took place.",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val interviewDate: LocalDate,

  @Schema(
    description = "What role the interviewee played in the incident or referral.",
  )
  val intervieweeRole: IntervieweeRole,

  @Schema(
    description = "Information provided in interview.",
  )
  val interviewText: String?,

  @Schema(
    description = "The date and time the Interview was created",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdAt: LocalDateTime,

  @Schema(
    description = "The username of the user who created the Interview",
    example = "USER1234",
  )
  val createdBy: String,

  @Schema(
    description = "The displayable name of the user who created the Interview",
    example = "Firstname Lastname",
  )
  val createdByDisplayName: String,

  @Schema(
    description = "The date and time the Interview was last modified",
    example = "2022-07-15T15:24:56",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val lastModifiedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who last modified the Interview",
    example = "USER1234",
  )
  val lastModifiedBy: String?,

  @Schema(
    description = "The displayable name of the user who last modified the Interview",
    example = "Firstname Lastname",
  )
  val lastModifiedByDisplayName: String?,
)