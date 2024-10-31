package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.data.domain.Sort.by
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.CELL_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.FIRST_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.LAST_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipSummary.Companion.STATUS_DESCRIPTION
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

  private fun sortByDate(direction: Direction) = by(direction, CsipSummary.REFERRAL_DATE, CsipSummary.ID)
  private fun sortByName(direction: Direction): Sort = by(direction, LAST_NAME, FIRST_NAME, PRISON_NUMBER)
  private fun tieBreaker() = sortByName(ASC).and(sortByDate(DESC))

  override fun buildSort(field: String, direction: Direction): Sort {
    return when (field) {
      REFERRAL_DATE -> sortByDate(direction).and(sortByName(ASC))
      NAME -> sortByName(direction).and(sortByDate(DESC))
      LOCATION -> by(direction, PRISON_CODE, CELL_LOCATION).and(tieBreaker())
      STATUS -> by(direction, STATUS_DESCRIPTION).and(tieBreaker())
      else -> by(direction, field).and(tieBreaker())
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
