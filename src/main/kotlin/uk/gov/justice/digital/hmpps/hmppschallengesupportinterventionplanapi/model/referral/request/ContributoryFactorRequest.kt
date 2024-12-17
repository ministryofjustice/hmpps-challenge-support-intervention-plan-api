package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "The request body to create a contributory factor to the incident that motivated the CSIP referral")
data class CreateContributoryFactorRequest(
  override val factorTypeCode: String,
  override val comment: String?,
) : ContributoryFactorRequest

@Schema(description = "The request body to update a contributory factor to the incident that motivated the CSIP referral")
data class UpdateContributoryFactorRequest(
  override val factorTypeCode: String,
  override val comment: String?,
) : ContributoryFactorRequest

@Schema(description = "The request body to update a contributory factor to the incident that motivated the CSIP referral")
data class MergeContributoryFactorRequest(
  override val factorTypeCode: String,
  override val comment: String?,
  val factorUuid: UUID?,
) : ContributoryFactorRequest
