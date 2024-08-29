package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSummaries
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CsipSummaryRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CsipRecordService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "1. CSIP Record Controller", description = "Endpoints for CSIP Record operations")
class CsipRecordsController(val csipRecordService: CsipRecordService, val prisonerSearchClient: PrisonerSearchClient) {
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
        responseCode = "400",
        description = "Bad Request",
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
  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/prisoners/{prisonNumber}/csip-records")
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun getCsipRecordsByPrisonNumbers(
    @PathVariable prisonNumber: String,
    @Valid @ParameterObject request: CsipSummaryRequest,
  ): CsipSummaries = csipRecordService.findCsipRecordsForPrisoner(prisonNumber, request)

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
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/prisoners/{prisonNumber}/csip-records")
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun createCsipRecord(
    @PathVariable @Parameter(description = "Prison Number of the prisoner", required = true) prisonNumber: String,
    @Valid @RequestBody createCsipRecordRequest: CreateCsipRecordRequest,
  ): CsipRecord {
    val prisoner = requireNotNull(prisonerSearchClient.getPrisoner(prisonNumber)) { "Prisoner number invalid" }
    return csipRecordService.createCsipRecord(prisoner, createCsipRecordRequest)
  }

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
  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/csip-records/{recordUuid}")
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI', '$ROLE_NOMIS')")
  fun retrieveCsipRecord(
    @PathVariable @Parameter(description = "CSIP record unique identifier", required = true) recordUuid: UUID,
  ): CsipRecord = csipRecordService.retrieveCsipRecord(recordUuid)

  @Operation(
    summary = "Update the log code for a CSIP record and/or optionally the referral.",
    description = "Update the log code for a CSIP record. Publishes person.csip.record.updated event with affected component of `Record`",
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
  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/csip-records/{recordUuid}")
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun updateCsipRecord(
    @PathVariable @Parameter(description = "CSIP record unique identifier", required = true) recordUuid: UUID,
    @Valid @RequestBody updateCsipRecordRequest: UpdateCsipRecordRequest,
  ): CsipRecord = csipRecordService.updateCsipRecord(recordUuid, updateCsipRecordRequest)

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
        responseCode = "200",
        description = "CSIP previously deleted or never existed",
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
  @DeleteMapping("/csip-records/{recordUuid}")
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun deleteCsipRecord(
    @PathVariable @Parameter(description = "CSIP record unique identifier", required = true) recordUuid: UUID,
  ): ResponseEntity<Unit> = when (csipRecordService.deleteCsipRecord(recordUuid)) {
    true -> ResponseEntity.noContent().build()
    false -> ResponseEntity.ok().build()
  }
}
