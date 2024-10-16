package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidInputException

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
