package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "The request for creating a CSIP Plan for a CSIP record")
data class CreatePlanRequest(
  override val caseManager: String,
  override val reasonForPlan: String,
  override val nextCaseReviewDate: LocalDate,
  override val identifiedNeeds: List<CreateIdentifiedNeedRequest> = listOf(),
) : PlanRequest,
  IdentifiedNeedsRequest

@Schema(description = "The request for updating a CSIP Plan for a CSIP record")
data class UpdatePlanRequest(
  override val caseManager: String?,
  override val reasonForPlan: String?,
  override val nextCaseReviewDate: LocalDate?,
) : PlanRequest
