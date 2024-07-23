package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CsipRecordService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "1. CSIP Record Controller", description = "Endpoints for CSIP Record operations")
class CsipRecordsController(val csipRecordService: CsipRecordService) {
  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/prisoners/{prisonNumber}/csip-records")
  @Operation(
    summary = "Retrieve and filter all CSIP records for a prisoner.",
    description = "Returns the CSIP records for a prisoner. Supports log code filtering.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "CSIP records found",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun getCsipRecordsByPrisonNumber(
    @PathVariable @Parameter(
      description = "Prison Number of the prisoner",
      required = true,
    ) prisonNumber: String,
    @RequestParam @Parameter(
      description = "Filter CSIP records that contain the search text in their Log Code. The search is case insensitive.",
      example = "Search text",
    ) logCode: String?,
    @RequestParam @Parameter(
      description = "Filter CSIP records that have a created timestamp at or after the supplied time.",
      example = "2021-09-27T14:19:25",
    ) createdAtStart: LocalDateTime?,
    @RequestParam @Parameter(
      description = "Filter CSIP records that have a created timestamp at or before the supplied time.",
      example = "2021-09-27T14:19:25",
    ) createdAtEnd: LocalDateTime?,
    @ParameterObject @PageableDefault(sort = ["createdAt"], direction = Direction.DESC) pageable: Pageable,
  ): Page<CsipRecord> = throw NotImplementedError()

  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/prisons/{prisonCode}/csip-records")
  @Operation(
    summary = "Retrieve and filter all CSIP records for prisoners resident in the prison.",
    description = "Returns the CSIP records for prisoners resident in the prison. Supports log code filtering.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "CSIP records found",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun getCsipRecordsByPrisonCode(
    @PathVariable @Parameter(
      description = "Prison Code of the prison",
      required = true,
    ) prisonCode: String,
    @RequestParam @Parameter(
      description = "Filter CSIP records that contain the search text in their Log Code. The search is case insensitive.",
      example = "Search text",
    ) logCode: String?,
    @RequestParam @Parameter(
      description = "Filter CSIP records that have a created timestamp at or after the supplied time.",
      example = "2021-09-27T14:19:25",
    ) createdAtStart: LocalDateTime?,
    @RequestParam @Parameter(
      description = "Filter CSIP records that have a created timestamp at or before the supplied time.",
      example = "2021-09-27T14:19:25",
    ) createdAtEnd: LocalDateTime?,
    @ParameterObject @PageableDefault(sort = ["createdAt"], direction = Direction.DESC) pageable: Pageable,
  ): Page<CsipRecord> = throw NotImplementedError()

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/prisoners/{prisonNumber}/csip-records")
  @Operation(
    summary = "Create a CSIP record for a prisoner.",
    description = "Create the CSIP record, referral and contributory factors. This starts the CSIP process. Publishes person.csip.record.created and person.csip.contributory-factor.created events",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "CSIP record created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI', '$ROLE_NOMIS')")
  fun createCsipRecord(
    @PathVariable @Parameter(
      description = "Prison Number of the prisoner",
      required = true,
    ) prisonNumber: String,
    @Valid @RequestBody createCsipRecordRequest: CreateCsipRecordRequest,
    httpRequest: HttpServletRequest,
  ): CsipRecord =
    csipRecordService.createCsipRecord(createCsipRecordRequest, prisonNumber, httpRequest.csipRequestContext())

  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/csip-records/{recordUuid}")
  @Operation(
    summary = "Get a CSIP record by its unique identifier",
    description = "Returns the CSIP record with the matching identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "CSIP record found",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The CSIP record associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI', '$ROLE_NOMIS')")
  fun retrieveCsipRecord(
    @PathVariable @Parameter(
      description = "CSIP record unique identifier",
      required = true,
    ) recordUuid: UUID,
  ): CsipRecord = csipRecordService.retrieveCsipRecord(recordUuid)

  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/csip-records/{recordUuid}")
  @Operation(
    summary = "Update the log code for a CSIP record.",
    description = "Update the log code for a CSIP record. Publishes person.csip.record.updated event with recordAffected = true",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "CSIP record updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The CSIP record associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI', '$ROLE_NOMIS')")
  fun updateCsipRecord(
    httpRequest: HttpServletRequest,
    @PathVariable @Parameter(
      description = "CSIP record unique identifier",
      required = true,
    ) recordUuid: UUID,
    @Valid @RequestBody updateCsipRecordRequest: UpdateCsipRecordRequest,
  ): CsipRecord =
    csipRecordService.updateCsipRecord(httpRequest.csipRequestContext(), recordUuid, updateCsipRecordRequest)

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/csip-records/{recordUuid}")
  @Operation(
    summary = "Delete a complete CSIP record.",
    description = "Delete the whole of a CSIP record, including its referral and plan. Publishes prisoner-csip.csip-record-deleted event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "CSIP record deleted",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The CSIP record associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun deleteCsipRecord(
    @PathVariable @Parameter(
      description = "CSIP record unique identifier",
      required = true,
    ) recordUuid: UUID,
  ): Nothing = throw NotImplementedError()
}
