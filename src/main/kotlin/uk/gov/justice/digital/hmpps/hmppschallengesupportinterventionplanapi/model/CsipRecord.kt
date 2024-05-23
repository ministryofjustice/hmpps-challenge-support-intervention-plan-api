package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(
  description = "A CSIP Record associated with a person",
)
data class CsipRecord(
  @Schema(
    description = "The unique identifier assigned to the CSIP Record",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val recordUuid: UUID,

  @Schema(
    description = "The prison number of the person the CSIP record is for.",
  )
  val prisonNumber: String,

  @Schema(
    description = "The prison code where the person was resident at the time the CSIP record was created.",
  )
  val prisonCodeWhenRecorded: String,

  @Schema(
    description = "User entered identifier for the CSIP record. Defaults to the prison code.",
  )
  val logNumber: String,

  @Schema(
    description = "The date and time the CSIP Record was created",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdAt: LocalDateTime,

  @Schema(
    description = "The username of the user who created the CSIP Record",
    example = "USER1234",
  )
  val createdBy: String,

  @Schema(
    description = "The displayable name of the user who created the CSIP Record",
    example = "Firstname Lastname",
  )
  val createdByDisplayName: String,

  @Schema(
    description = "The date and time the CSIP Record was last modified",
    example = "2022-07-15T15:24:56",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val lastModifiedAt: LocalDateTime?,

  @Schema(
    description = "The username of the user who last modified the CSIP Record",
    example = "USER1234",
  )
  val lastModifiedBy: String?,

  @Schema(
    description = "The displayable name of the user who last modified the CSIP Record",
    example = "Firstname Lastname",
  )
  val lastModifiedByDisplayName: String?,

  @Schema(
    description = "The referral that results in the creation of this CSIP record.",
  )
  val referral: Referral,

  @Schema(
    description = "The CSIP Plan of this CSIP record.",
  )
  val plan: Plan?,
)
