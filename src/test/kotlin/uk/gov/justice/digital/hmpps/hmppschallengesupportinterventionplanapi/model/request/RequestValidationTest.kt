package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat

abstract class RequestValidationTest {
  protected val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  protected fun assertSingleValidationError(
    validate: MutableSet<ConstraintViolation<Any>>,
    propertyName: String,
    message: String,
  ) {
    assertThat(validate.size).isEqualTo(1)
    assertThat(validate.first().propertyPath.toString()).isEqualTo(propertyName)
    assertThat(validate.first().message).isEqualTo(message)
  }

  protected fun assertValidationErrors(
    validate: MutableSet<ConstraintViolation<Any>>,
    vararg errors: Pair<String, String>,
  ) {
    val validationErrors = validate.map { violation -> Pair(violation.propertyPath.toString(), violation.message) }
    assertThat(validationErrors).containsExactlyInAnyOrder(*errors)
  }
}
