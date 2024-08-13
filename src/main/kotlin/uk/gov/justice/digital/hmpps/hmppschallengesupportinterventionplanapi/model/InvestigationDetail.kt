package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.InvestigationRequest
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [InvestigationRequestValidator::class])
annotation class ValidInvestigationDetail(
  val message: String = "At least one of staffInvolved, evidenceSecured, occurrenceReason, personsUsualBehaviour, personsTrigger, protectiveFactors must be non null.",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
)

class InvestigationRequestValidator : ConstraintValidator<ValidInvestigationDetail, InvestigationRequest> {
  override fun isValid(request: InvestigationRequest, context: ConstraintValidatorContext): Boolean {
    return with(request) {
      listOfNotNull(
        staffInvolved,
        evidenceSecured,
        occurrenceReason,
        personsUsualBehaviour,
        personsTrigger,
        protectiveFactors,
      ).isNotEmpty()
    }
  }
}
