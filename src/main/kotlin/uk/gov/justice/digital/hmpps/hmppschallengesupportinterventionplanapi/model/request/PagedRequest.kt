package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidInputException
import java.time.LocalDateTime

interface PagedRequest {
  val page: Int
  val size: Int
  val sort: String

  fun validSortFields(): Set<String> = setOf("createdAt")

  fun sort(): Sort {
    val validate: (String) -> String = {
      if (it in validSortFields()) it else throw InvalidInputException("sort", it)
    }
    val split = sort.split(",")
    val (field, direction) = when (split.size) {
      1 -> validate(split[0]) to Sort.Direction.DESC
      else -> validate(split[0]) to if (split[1].lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC
    }
    return Sort.by(direction, field)
  }

  fun pageable(): Pageable = PageRequest.of(page - 1, size, sort())
}

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
