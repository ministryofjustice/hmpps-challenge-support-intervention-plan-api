package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.InterviewsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.InvestigationRequest
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [InvestigationRequestValidator::class])
annotation class ValidInvestigationDetail(
  val message: String = DEFAULT_MESSAGE,
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
) {
  companion object {
    const val DEFAULT_MESSAGE = "At least one of staffInvolved, evidenceSecured, occurrenceReason, personsUsualBehaviour, personsTrigger, protectiveFactors must be non null"
    const val WITH_INTERVIEW_MESSAGE = "$DEFAULT_MESSAGE or at least one interview must be provided"
  }
}

class InvestigationRequestValidator : ConstraintValidator<ValidInvestigationDetail, InvestigationRequest> {
  override fun isValid(request: InvestigationRequest, context: ConstraintValidatorContext): Boolean {
    return with(request) {
      val oneFieldNotNull = listOfNotNull(
        staffInvolved,
        evidenceSecured,
        occurrenceReason,
        personsUsualBehaviour,
        personsTrigger,
        protectiveFactors,
      ).isNotEmpty()

      val atLeastOneInterview = request is InterviewsRequest && request.interviews.isNotEmpty()

      oneFieldNotNull || atLeastOneInterview
    }
  }
}
