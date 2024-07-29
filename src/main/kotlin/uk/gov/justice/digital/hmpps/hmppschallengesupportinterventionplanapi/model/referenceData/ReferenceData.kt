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
    description = "The date and time the code was deactivated",
    example = "2023-11-08T09:53:34",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val deactivatedAt: LocalDateTime?,
)
