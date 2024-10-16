package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The request body to create a Attendee/Contributor to the review of a CSIP Plan")
data class CreateAttendeeRequest(
  override val name: String?,
  override val role: String?,
  override val isAttended: Boolean?,
  override val contribution: String?,
) : AttendeeRequest

@Schema(description = "The request body to update a Attendee/Contributor to the review of a CSIP Plan")
data class UpdateAttendeeRequest(
  override val name: String?,
  override val role: String?,
  override val isAttended: Boolean?,
  override val contribution: String?,
) : AttendeeRequest
