package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidDomainException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingPrerequisiteResourceException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException

@RestControllerAdvice
class HmppsChallengeSupportInterventionPlanApiExceptionHandler {
  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<uk.gov.justice.hmpps.kotlin.common.ErrorResponse> =
    ResponseEntity.status(HttpStatus.FORBIDDEN).body(
      uk.gov.justice.hmpps.kotlin.common.ErrorResponse(
        status = HttpStatus.FORBIDDEN.value(),
        userMessage = "Authentication problem. Check token and roles - ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Access denied exception: {}", e.message) }

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<uk.gov.justice.hmpps.kotlin.common.ErrorResponse> =
    ResponseEntity.status(BAD_REQUEST).body(
      uk.gov.justice.hmpps.kotlin.common.ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Missing servlet request parameter exception: {}", e.message) }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<uk.gov.justice.hmpps.kotlin.common.ErrorResponse> {
    val cause = e.cause?.cause
    if (cause is InvalidDomainException) {
      return handleInvalidDomainException(cause)
    }

    val type = e.requiredType
    val message = if (type.isEnum) {
      "Parameter ${e.name} must be one of the following ${StringUtils.join(type.enumConstants, ", ")}"
    } else {
      "Parameter ${e.name} must be of type ${type.typeName}"
    }

    return ResponseEntity.status(BAD_REQUEST).body(
      uk.gov.justice.hmpps.kotlin.common.ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: $message",
        developerMessage = e.message,
      ),
    ).also { log.info("Method argument type mismatch exception: {}", e.message) }
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<uk.gov.justice.hmpps.kotlin.common.ErrorResponse> =
    ResponseEntity.status(BAD_REQUEST).body(
      uk.gov.justice.hmpps.kotlin.common.ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: Couldn't read request body",
        developerMessage = e.message,
      ),
    ).also { log.info("HTTP message not readable exception: {}", e.message) }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<uk.gov.justice.hmpps.kotlin.common.ErrorResponse> =
    ResponseEntity.status(BAD_REQUEST).body(
      uk.gov.justice.hmpps.kotlin.common.ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure(s): ${
          e.allErrors.map { it.defaultMessage }.distinct().sorted().joinToString("\n")
        }",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  @ExceptionHandler(IllegalArgumentException::class)
  fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<uk.gov.justice.hmpps.kotlin.common.ErrorResponse> =
    ResponseEntity.status(BAD_REQUEST).body(
      uk.gov.justice.hmpps.kotlin.common.ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Illegal argument exception: {}", e.message) }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<uk.gov.justice.hmpps.kotlin.common.ErrorResponse> =
    ResponseEntity.status(BAD_REQUEST).body(
      uk.gov.justice.hmpps.kotlin.common.ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  @ExceptionHandler(HandlerMethodValidationException::class)
  fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<uk.gov.justice.hmpps.kotlin.common.ErrorResponse> =
    e.allErrors.map { it.toString() }.distinct().sorted().joinToString("\n").let { validationErrors ->
      ResponseEntity.status(BAD_REQUEST).body(
        uk.gov.justice.hmpps.kotlin.common.ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure(s): ${
            e.allErrors.map { it.defaultMessage }.distinct().sorted().joinToString("\n")
          }",
          developerMessage = "${e.message} $validationErrors",
        ),
      ).also { log.info("Validation exception: $validationErrors\n {}", e.message) }
    }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> =
    ResponseEntity.status(NOT_FOUND).body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(InvalidDomainException::class)
  fun handleInvalidDomainException(e: InvalidDomainException): ResponseEntity<uk.gov.justice.hmpps.kotlin.common.ErrorResponse> =
    ResponseEntity.status(NOT_FOUND).body(
      uk.gov.justice.hmpps.kotlin.common.ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<uk.gov.justice.hmpps.kotlin.common.ErrorResponse> =
    ResponseEntity.status(NOT_FOUND).body(
      uk.gov.justice.hmpps.kotlin.common.ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(ResourceAlreadyExistException::class)
  fun handleResourceAlreadyExistException(e: ResourceAlreadyExistException): ResponseEntity<uk.gov.justice.hmpps.kotlin.common.ErrorResponse> =
    ResponseEntity.status(CONFLICT).body(
      uk.gov.justice.hmpps.kotlin.common.ErrorResponse(
        status = CONFLICT,
        userMessage = "Conflict failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Conflict failure: {}", e.message) }

  @ExceptionHandler(MissingPrerequisiteResourceException::class)
  fun handleMissingPrerequisiteResourceException(e: MissingPrerequisiteResourceException): ResponseEntity<uk.gov.justice.hmpps.kotlin.common.ErrorResponse> =
    ResponseEntity.status(BAD_REQUEST).body(
      uk.gov.justice.hmpps.kotlin.common.ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Invalid request: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Invalid request: {}", e.message) }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> =
    ResponseEntity.status(INTERNAL_SERVER_ERROR).body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.error("Unexpected exception", e) }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) : this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
