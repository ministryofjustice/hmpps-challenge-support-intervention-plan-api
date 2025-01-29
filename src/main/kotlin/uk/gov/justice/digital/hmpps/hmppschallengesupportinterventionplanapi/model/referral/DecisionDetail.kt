package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.DecisionAndActionsRequest
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [DecisionRequestValidator::class])
annotation class ValidDecisionDetail(
  val message: String = "At least one decision field or at least one action must be provided",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
)

class DecisionRequestValidator : ConstraintValidator<ValidDecisionDetail, DecisionAndActionsRequest> {
  override fun isValid(request: DecisionAndActionsRequest, context: ConstraintValidatorContext): Boolean = with(request) {
    listOfNotNull(
      conclusion,
      outcomeTypeCode,
      signedOffByRoleCode,
      nextSteps,
      actionOther,
    ).isNotEmpty() ||
      actions.isNotEmpty()
  }
}
