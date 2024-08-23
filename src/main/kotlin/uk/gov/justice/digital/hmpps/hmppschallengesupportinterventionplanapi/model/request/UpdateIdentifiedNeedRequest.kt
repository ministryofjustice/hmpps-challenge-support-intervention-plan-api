package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "The request body to update an Identified Need in a CSIP Plan")
data class UpdateIdentifiedNeedRequest(
  @Schema(description = "Details of the need identified in the CSIP plan.")
  @field:Size(min = 0, max = 1000, message = "Identified Need must be <= 1000 characters")
  override val identifiedNeed: String,

  @Schema(description = "The name of the person who is responsible for taking action on the intervention.")
  @field:Size(min = 0, max = 100, message = "Responsible person name must be <= 100 characters")
  override val responsiblePerson: String,

  @Schema(description = "The date the need was identified.", example = "2021-09-27")
  @JsonFormat(pattern = "yyyy-MM-dd")
  override val createdDate: LocalDate,

  @Schema(description = "The target date the need should be progressed or resolved.", example = "2021-09-27")
  @JsonFormat(pattern = "yyyy-MM-dd")
  override val targetDate: LocalDate,

  @Schema(description = "The date the identified need was resolved or closed.", example = "2021-09-27")
  @JsonFormat(pattern = "yyyy-MM-dd")
  override val closedDate: LocalDate?,

  @Schema(description = "The planned intervention for the identified need.")
  @field:Size(min = 0, max = 4000, message = "Intervention must be <= 4000 characters")
  override val intervention: String,

  @Schema(description = "How the plan to address the identified need is progressing.")
  @field:Size(min = 0, max = 4000, message = "Progression must be <= 4000 characters")
  override val progression: String?,
) : IdentifiedNeedRequest
