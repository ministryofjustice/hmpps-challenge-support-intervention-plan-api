package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.Referral
import java.util.UUID

@Schema(description = "A CSIP Record associated with a person")
data class CsipRecord(
  @Schema(
    description = "The unique identifier assigned to the CSIP Record",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val recordUuid: UUID,

  @Schema(description = "The prison number of the person the CSIP record is for.")
  val prisonNumber: String,

  @Schema(description = "The prison code where the person was resident at the time the CSIP record was created.")
  val prisonCodeWhenRecorded: String?,

  @Schema(description = "User entered identifier for the CSIP record. Defaults to the prison code.")
  val logCode: String?,

  @Schema(description = "The referral that results in the creation of this CSIP record.")
  val referral: Referral,

  @Schema(description = "The CSIP Plan of this CSIP record.")
  val plan: Plan?,

  @Schema(description = "The current status of the CSIP record.")
  val status: ReferenceData,
)
