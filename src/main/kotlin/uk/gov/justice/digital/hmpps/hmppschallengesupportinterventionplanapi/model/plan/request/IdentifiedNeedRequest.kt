package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "The request body to create an Identified Need in a CSIP Plan")
data class CreateIdentifiedNeedRequest(
  override val identifiedNeed: String,
  override val responsiblePerson: String,
  override val createdDate: LocalDate,
  override val targetDate: LocalDate,
  override val closedDate: LocalDate?,
  override val intervention: String,
  override val progression: String?,
) : IdentifiedNeedRequest

@Schema(description = "The request body to update an Identified Need in a CSIP Plan")
data class UpdateIdentifiedNeedRequest(
  override val identifiedNeed: String,
  override val responsiblePerson: String,
  override val createdDate: LocalDate,
  override val targetDate: LocalDate,
  override val closedDate: LocalDate?,
  override val intervention: String,
  override val progression: String?,
) : IdentifiedNeedRequest
