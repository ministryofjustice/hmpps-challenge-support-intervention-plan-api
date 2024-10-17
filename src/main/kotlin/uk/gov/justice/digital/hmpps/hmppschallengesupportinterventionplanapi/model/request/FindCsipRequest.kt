package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus

data class FindCsipRequest(
  @Parameter(description = "The prison code for the location of the prisoner", example = "MDI")
  val prisonCode: String,
  @Parameter(description = "The name(s) of the prisoner space separated", example = "First Last")
  val query: String?,
  @Parameter(description = "The status of the CSIP record", example = "CSIP_OPEN")
  val status: CsipStatus?,

  override val page: Int = 1,
  override val size: Int = 10,

  @Parameter(description = "The sort to apply to the results", example = "referralDate,desc")
  override val sort: String = "referralDate,desc",
) : PagedRequest {
  override fun validSortFields(): Set<String> =
    setOf(CASE_MANAGER, LOCATION, NAME, NEXT_REVIEW_DATE, REFERRAL_DATE, STATUS)

  override fun buildSort(field: String, direction: Direction): Sort =
    when (field) {
      CASE_MANAGER -> Sort.by(direction, "plan_caseManager")
      LOCATION -> Sort.by(direction, "personSummary_cellLocation")
      NAME -> Sort.by(direction, "personSummary_firstName", "personSummary_lastName", "personSummary_prisonNumber")
      NEXT_REVIEW_DATE -> Sort.by(direction, "plan_reviews_nextReviewDate", "plan_firstCaseReviewDate")
      REFERRAL_DATE -> Sort.by(direction, "referral_referralDate")
      else -> Sort.by(direction, field)
    }

  companion object {
    const val CASE_MANAGER = "caseManager"
    const val LOCATION = "location"
    const val NAME = "name"
    const val NEXT_REVIEW_DATE = "nextReviewDate"
    const val REFERRAL_DATE = "referralDate"
    const val STATUS = "status"
  }
}
