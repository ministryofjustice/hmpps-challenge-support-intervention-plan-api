package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CsipSummaryRequest(
  @Parameter(
    description = "Filter CSIP records that contain the search text in their Log Code. The search is case insensitive.",
    example = "Search text",
  )
  @field:Size(max = 10, message = "Log code must be <= 10 characters")
  val logCode: String?,

  @Parameter(
    description = "Filter CSIP records that have a created timestamp after the supplied time.",
    example = "2021-09-27T14:19:25",
  )
  val createdAtStart: LocalDateTime?,

  @Parameter(
    description = "Filter CSIP records that have a created timestamp before the supplied time.",
    example = "2021-09-27T14:19:25",
  )
  val createdAtEnd: LocalDateTime?,

  override val page: Int = 1,
  override val size: Int = 10,

  @Parameter(description = "The sort to apply to the results", example = "createdAt,desc")
  override val sort: String = "createdAt,desc",
) : PagedRequest
