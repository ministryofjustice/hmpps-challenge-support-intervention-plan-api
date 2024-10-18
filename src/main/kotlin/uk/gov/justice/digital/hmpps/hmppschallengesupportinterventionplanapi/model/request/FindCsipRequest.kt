package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus

data class FindCsipRequest(
  @Parameter(description = "The prison code for the location of the prisoner", example = "MDI")
  val prisonCode: String,
  @Parameter(
    description = "Either the prison number or the name(s) of the prisoner space separated",
    example = "First Last",
  )
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

  override fun buildSort(field: String, direction: Direction): Sort {
    val primary = when (field) {
      NAME -> Sort.by(direction, "firstName", "lastName", "prisonNumber")
      LOCATION -> Sort.by(direction, "prisonCode", "cellLocation")
      STATUS -> Sort.by(direction, "priority")
      else -> Sort.by(direction, field)
    }
    return if (field != REFERRAL_DATE) {
      primary.and(Sort.by(Direction.DESC, REFERRAL_DATE))
    } else {
      primary
    }
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
