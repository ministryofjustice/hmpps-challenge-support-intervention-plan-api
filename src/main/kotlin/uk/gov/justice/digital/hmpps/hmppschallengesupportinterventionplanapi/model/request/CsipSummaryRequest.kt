package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
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

  @Parameter(description = "The page to request, starting at 1", example = "1")
  @field:Min(value = 1, message = "Page number must be at least 1")
  override val page: Int = 1,

  @Parameter(description = "The page size to request", example = "10")
  @field:Min(value = 1, message = "Page size must be at least 1")
  @field:Max(value = 100, message = "Page size must not be more than 100")
  override val size: Int = 10,

  @Parameter(description = "The sort to apply to the results", example = "createdAt,desc")
  override val sort: String = "createdAt,desc",
) : PagedRequest
