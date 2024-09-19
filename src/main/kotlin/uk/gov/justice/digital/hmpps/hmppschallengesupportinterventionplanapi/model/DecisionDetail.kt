package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.DecisionAndActionsRequest
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [DecisionRequestValidator::class])
annotation class ValidDecisionDetail(
  val message: String = "Either outcome type code or conclusion must be provided",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
)

class DecisionRequestValidator : ConstraintValidator<ValidDecisionDetail, DecisionAndActionsRequest> {
  override fun isValid(request: DecisionAndActionsRequest, context: ConstraintValidatorContext): Boolean {
    return with(request) { listOfNotNull(outcomeTypeCode, conclusion).isNotEmpty() }
  }
}
