package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ClosedDateValidator::class])
annotation class CloseDateIfClosing(
  val message: String = "Closing a CSIP record requires both the action and closed date to be provided.",
  val groups: Array<KClass<out Any>> = [],
  val payload: Array<KClass<out Any>> = [],
)

class ClosedDateValidator : ConstraintValidator<CloseDateIfClosing, ReviewRequest> {
  override fun isValid(request: ReviewRequest, context: ConstraintValidatorContext): Boolean =
    (request.actions.contains(ReviewAction.CLOSE_CSIP) && request.csipClosedDate != null) ||
      request.csipClosedDate == null && !request.actions.contains(ReviewAction.CLOSE_CSIP)
}
