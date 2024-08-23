package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.springframework.context.MessageSourceResolvable
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidUserRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingComponentException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.MissingPrerequisiteResourceException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.ResourceAlreadyExistException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestControllerAdvice
class HmppsChallengeSupportInterventionPlanApiExceptionHandler {
  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> =
    ResponseEntity.status(HttpStatus.FORBIDDEN).body(
      ErrorResponse(
        status = HttpStatus.FORBIDDEN.value(),
        userMessage = "Authentication problem. Check token and roles - ${e.message}",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> =
    ResponseEntity.status(BAD_REQUEST).body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
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
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: $message",
        developerMessage = e.message,
      ),
    )
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
    ResponseEntity.status(BAD_REQUEST).body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: Couldn't read request body",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> =
    e.allErrors.mapErrors()

  @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
  fun handleIllegalArgumentOrStateException(e: RuntimeException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.devMessage(),
      ),
    )

  private fun RuntimeException.devMessage(): String = when (this) {
    is InvalidUserRequest -> "Details => $name:$value"
    else -> message ?: "${this::class.simpleName}: ${cause?.message ?: ""}"
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.devMessage(),
      ),
    )

  @ExceptionHandler(HandlerMethodValidationException::class)
  fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<ErrorResponse> =
    e.allErrors.mapErrors()

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> =
    ResponseEntity.status(NOT_FOUND).body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(InvalidDomainException::class)
  fun handleInvalidDomainException(e: InvalidDomainException): ResponseEntity<ErrorResponse> =
    ResponseEntity.status(NOT_FOUND).body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(NotFoundException::class)
  fun handleNotFoundException(e: NotFoundException): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND.value(),
          userMessage = "Not found: ${e.message}",
          developerMessage = "${e.resource} not found with identifier ${e.identifier}",
        ),
      )

  @ExceptionHandler(ResourceAlreadyExistException::class)
  fun handleResourceAlreadyExistException(e: ResourceAlreadyExistException): ResponseEntity<ErrorResponse> =
    ResponseEntity.status(CONFLICT).body(
      ErrorResponse(
        status = CONFLICT,
        userMessage = "Conflict failure: ${e.message}",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(MissingPrerequisiteResourceException::class)
  fun handleMissingPrerequisiteResourceException(e: MissingPrerequisiteResourceException): ResponseEntity<ErrorResponse> =
    ResponseEntity.status(BAD_REQUEST).body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Invalid request: ${e.message}",
        developerMessage = e.message,
        moreInfo = if (e is MissingComponentException) e.recordUuid.toString() else null,
      ),
    )

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> =
    ResponseEntity.status(INTERNAL_SERVER_ERROR).body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = "${e::class.simpleName} => ${e.message}",
      ),
    )

  private fun List<MessageSourceResolvable>.mapErrors() =
    map { it.defaultMessage }.distinct().sorted().let {
      val validationFailure = "Validation failure"
      val message = if (it.size > 1) {
        """
              |${validationFailure}s: 
              |${it.joinToString(System.lineSeparator())}
              |
        """.trimMargin()
      } else {
        "$validationFailure: ${it.joinToString(System.lineSeparator())}"
      }
      ResponseEntity
        .status(BAD_REQUEST)
        .body(
          ErrorResponse(
            status = BAD_REQUEST,
            userMessage = message,
            developerMessage = "400 BAD_REQUEST $message",
          ),
        )
    }
}
